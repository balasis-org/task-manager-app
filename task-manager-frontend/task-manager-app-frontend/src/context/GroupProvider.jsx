import { useState, useEffect, useCallback, useContext, useRef } from "react";
import { GroupContext } from "./GroupContext";
import { AuthContext } from "./AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import { encryptForCache, decryptFromCache, cacheMatchesKey } from "@assets/js/cacheCrypto.js";

/*
 * ─── DTO Field Reference (backend sends minified JSON keys) ──────────
 *
 * GroupWithPreviewDto (full group detail — GET /api/groups/{id}):
 *   n    = name              d    = description        an   = announcement
 *   diu  = defaultImgUrl     iu   = imgUrl             oi   = ownerId
 *   on   = ownerName         ca   = createdAt          aen  = allowEmailNotification
 *   lged = lastGroupEventDate
 *   tp   = taskPreviews (array of TaskPreviewDto)
 *
 * TaskPreviewDto (each task card on the dashboard):
 *   i  = id        t  = title      ts = taskState     ca = createdAt
 *   dd = dueDate   cc = commentCount  a = accessible  nc = newCommentsToBeRead
 *   cn = creatorName  p = priority   dl = deletable
 *
 * GroupRefreshDto (delta — GET /api/groups/{id}/refresh?lastSeen=):
 *   sn  = serverNow   c   = changed      mc  = membersChanged
 *   ct  = changedTasks (array of TaskPreviewDto)
 *   dti = deletedTaskIds (array of Long)
 *   ...plus same group fields as above for anything that changed
 * ─────────────────────────────────────────────────────────────────────
 */


// =====================================================================
// LocalStorage helpers
// =====================================================================

const STORAGE_PREFIX     = "tm_u";
const MAX_CACHED_USERS   = 3;
const USER_ORDER_KEY     = "tm_user_order";

// -- low-level wrappers (one place to swap if we ever move to sessionStorage) --

function storageGetRaw(key)        { return localStorage.getItem(key); }
function storageSetRaw(key, value) { localStorage.setItem(key, value); }
function storageGetJson(key)       { try { return JSON.parse(localStorage.getItem(key)); } catch { return null; } }
function storageSetJson(key, val)  { localStorage.setItem(key, JSON.stringify(val)); }
function storageRemove(key)        { localStorage.removeItem(key); }

// -- per-user key builders --

function userPrefix(userId)                  { return userId ? `${STORAGE_PREFIX}${userId}_` : ""; }
function groupsListKey(userId)               { return `${userPrefix(userId)}groups`; }
function activeGroupIdKey(userId)            { return `${userPrefix(userId)}active_group`; }
function groupDetailKey(userId, groupId)     { return `${userPrefix(userId)}gd_${groupId}`; }
function groupLastSeenKey(userId, groupId)   { return `${userPrefix(userId)}ls_${groupId}`; }


// =====================================================================
// Multi-user eviction — keeps at most MAX_CACHED_USERS sets of data.
// Most-recently-used user is at index 0.  When a new user pushes the
// count past the cap, the oldest user's keys are wiped.
// =====================================================================

function getUserOrder() {
    try { return JSON.parse(localStorage.getItem(USER_ORDER_KEY)) || []; }
    catch { return []; }
}

function markUserActive(userId) {
    const order = getUserOrder().filter(id => id !== userId);
    order.unshift(userId);
    localStorage.setItem(USER_ORDER_KEY, JSON.stringify(order));
    evictOldUsersIfNeeded(order);
}

function evictOldUsersIfNeeded(order) {
    while (order.length > MAX_CACHED_USERS) {
        const oldestUser = order.pop();
        purgeAllUserData(oldestUser);
    }
    localStorage.setItem(USER_ORDER_KEY, JSON.stringify(order));
}

/** Removes every LS key that belongs to the given user. */
function purgeAllUserData(userId) {
    const prefix = userPrefix(userId);
    const keysToRemove = [];
    for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith(prefix)) keysToRemove.push(key);
    }
    keysToRemove.forEach(k => localStorage.removeItem(k));
}


// =====================================================================
// Delta merge — applies a GroupRefreshDto on top of a cached
// GroupWithPreviewDto to produce an up-to-date detail object.
//
// Only touches fields the server flagged as changed. Task previews
// get new/updated entries merged in, and deleted ones removed.
// =====================================================================

function applyDeltaToDetail(cachedDetail, delta) {
    if (!delta.c) return cachedDetail;

    const merged = { ...cachedDetail };

    // Group-level fields (only overwrite if the server sent a value)
    if (delta.n    !== undefined && delta.n    !== null) merged.n    = delta.n;
    if (delta.d    !== undefined && delta.d    !== null) merged.d    = delta.d;
    if (delta.an   !== undefined)                       merged.an   = delta.an;
    if (delta.diu  !== undefined)                       merged.diu  = delta.diu;
    if (delta.iu   !== undefined)                       merged.iu   = delta.iu;
    if (delta.aen  !== undefined && delta.aen !== null)  merged.aen  = delta.aen;
    if (delta.lged !== undefined)                       merged.lged = delta.lged;

    // Task previews — remove deleted, upsert changed
    let tasks = [...(merged.tp ?? [])];

    if (delta.dti?.length) {
        const deletedIds = new Set(delta.dti);
        tasks = tasks.filter(task => !deletedIds.has(task.i));
    }

    if (delta.ct?.length) {
        const changedById = new Map(delta.ct.map(task => [task.i, task]));

        // Replace existing tasks that were updated
        tasks = tasks.map(task => changedById.has(task.i) ? changedById.get(task.i) : task);

        // Append brand-new tasks (not already in the list)
        const existingIds = new Set(tasks.map(task => task.i));
        for (const task of delta.ct) {
            if (!existingIds.has(task.i)) tasks.push(task);
        }
    }

    merged.tp = tasks;
    return merged;
}


// =====================================================================
// GroupProvider — central state for groups, active selection, detail,
// members, polling, and localStorage cache management.
// =====================================================================

export default function GroupProvider({ children }) {
    const { user } = useContext(AuthContext);

    // The cacheKey rotates every 7 days on the server and is used to
    // derive the AES encryption key for localStorage blobs.
    const cacheKeyRef = useRef(null);
    cacheKeyRef.current = user?.cacheKey || null;

    // ── Core state ──────────────────────────────────────────────────
    const [groups, setGroups]               = useState([]);
    const [activeGroup, setActiveGroup]     = useState(null);
    const [groupDetail, setGroupDetail]     = useState(null);
    const [members, setMembers]             = useState([]);
    const [loadingGroups, setLoadingGroups] = useState(true);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [myRole, setMyRole]               = useState(null);

    // Prevents re-fetching members when we already have them for this group
    const membersLoadedForGroupRef = useRef(null);

    // Prevents the activeGroup effect from double-firing loadOrRefreshDetail
    // when loadGroups() sets activeGroup twice (cache then API) with the same ID
    // but different object references.
    const detailLoadedForGroupRef = useRef(null);


    // =================================================================
    // Effect: clear everything when user logs out, load when user arrives
    // =================================================================

    useEffect(() => {
        if (!user) {
            cacheKeyRef.current = null;
            setGroups([]);
            setActiveGroup(null);
            setGroupDetail(null);
            setMembers([]);
            setMyRole(null);
            setLoadingGroups(false);
            return;
        }

        loadGroupsList();
    }, [user?.id]);


    // =================================================================
    // Effect: when active group changes, load its detail (if not already loaded)
    // =================================================================

    useEffect(() => {
        if (activeGroup && user) {
            storageSetJson(activeGroupIdKey(user.id), activeGroup.id);

            // Skip if we already dispatched detail loading for this exact group
            // (happens when cache hydration and API refresh both set activeGroup)
            if (detailLoadedForGroupRef.current !== activeGroup.id) {
                detailLoadedForGroupRef.current = activeGroup.id;
                loadOrRefreshGroupDetail(activeGroup.id);
            }
        } else {
            detailLoadedForGroupRef.current = null;
            setGroupDetail(null);
            setMembers([]);
            setMyRole(null);
        }
    }, [activeGroup]);


    // =================================================================
    // Effect: derive current user's role from the members list
    // =================================================================

    useEffect(() => {
        if (user && members.length > 0) {
            const myMembership = members.find(m => m.user?.id === user.id);
            setMyRole(myMembership?.role ?? null);
        } else {
            setMyRole(null);
        }
    }, [members, user]);


    // =================================================================
    // loadGroupsList — stale-while-revalidate for the sidebar
    //
    // 1) Instantly show groups from encrypted LS cache (no spinner).
    // 2) Always fetch fresh list from API in the background.
    // 3) If API succeeds, overwrite cache + re-resolve active group.
    // =================================================================

    async function loadGroupsList() {
        setLoadingGroups(true);

        const encryptionKey       = cacheKeyRef.current;
        const storedGroupsBlob    = storageGetRaw(groupsListKey(user.id));
        let   cachedGroupsList    = null;

        // Try to decrypt the cached groups list
        if (encryptionKey && storedGroupsBlob && cacheMatchesKey(encryptionKey, storedGroupsBlob)) {
            cachedGroupsList = await decryptFromCache(encryptionKey, storedGroupsBlob);
        }

        // Show cached data immediately if available
        if (Array.isArray(cachedGroupsList) && cachedGroupsList.length > 0) {
            setGroups(cachedGroupsList);
            setLoadingGroups(false);

            const savedActiveId = storageGetJson(activeGroupIdKey(user.id));
            const matchedGroup  = cachedGroupsList.find(g => g.id === savedActiveId);
            setActiveGroup(matchedGroup || cachedGroupsList[0]);
        }

        // Always hit the API to get the real list
        try {
            const freshGroups = await apiGet("/api/groups");
            const groupsList  = Array.isArray(freshGroups) ? freshGroups : [];
            setGroups(groupsList);

            // Persist the fresh list into encrypted LS
            if (encryptionKey) {
                const encrypted = await encryptForCache(encryptionKey, groupsList);
                storageSetRaw(groupsListKey(user.id), encrypted);
            }

            markUserActive(user.id);

            // Re-resolve active group from fresh data
            const savedActiveId = storageGetJson(activeGroupIdKey(user.id));
            const matchedGroup  = groupsList.find(g => g.id === savedActiveId);
            if (matchedGroup)          setActiveGroup(matchedGroup);
            else if (groupsList.length) setActiveGroup(groupsList[0]);
            else                        setActiveGroup(null);
        } catch {
            // API failed — keep cached data if we had any, otherwise show empty
            if (!cachedGroupsList) setGroups([]);
        } finally {
            setLoadingGroups(false);
        }
    }


    // =================================================================
    // loadOrRefreshGroupDetail — loads the dashboard data for a group
    //
    // If we have a cached detail + lastSeen timestamp, we use the delta
    // endpoint (/refresh?lastSeen=) which returns only what changed.
    // Otherwise we do a full fetch of detail + members.
    // =================================================================

    async function loadOrRefreshGroupDetail(groupId) {
        const encryptionKey = cacheKeyRef.current;

        // Try to load cached detail from LS
        let   cachedDetail       = null;
        const storedDetailBlob   = storageGetRaw(groupDetailKey(user.id, groupId));
        const storedLastSeen     = storageGetJson(groupLastSeenKey(user.id, groupId));

        if (encryptionKey && storedDetailBlob && cacheMatchesKey(encryptionKey, storedDetailBlob)) {
            cachedDetail = await decryptFromCache(encryptionKey, storedDetailBlob);
        }

        // Guard against stale cache format — if the old key name "taskPreviews"
        // exists but the new minified "tp" doesn't, the cache is from a previous
        // format version. Wipe it and start fresh.
        if (cachedDetail && cachedDetail.taskPreviews !== undefined && cachedDetail.tp === undefined) {
            cachedDetail = null;
            storageRemove(groupDetailKey(user.id, groupId));
            storageRemove(groupLastSeenKey(user.id, groupId));
        }

        // Show cached detail instantly while we check for updates
        if (cachedDetail) setGroupDetail(cachedDetail);

        try {
            if (cachedDetail && storedLastSeen) {
                // ── Delta refresh path ──────────────────────────────
                const delta = await apiGet(
                    `/api/groups/${groupId}/refresh?lastSeen=${encodeURIComponent(storedLastSeen)}`
                );

                // Reload members if the server says they changed, or if we
                // haven't loaded members for this group yet
                if (delta.mc || membersLoadedForGroupRef.current !== groupId) {
                    const membersPage = await apiGet(
                        `/api/groups/${groupId}/groupMemberships?page=0&size=100`
                    );
                    setMembers(membersPage?.content ?? []);
                    membersLoadedForGroupRef.current = groupId;
                }

                // Merge the delta into the cached detail
                const updatedDetail = delta.c
                    ? applyDeltaToDetail(cachedDetail, delta)
                    : cachedDetail;
                setGroupDetail(updatedDetail);

                // Persist the merged result back to LS
                if (encryptionKey) {
                    const encrypted = await encryptForCache(encryptionKey, updatedDetail);
                    storageSetRaw(groupDetailKey(user.id, groupId), encrypted);
                }
                storageSetJson(groupLastSeenKey(user.id, groupId), delta.sn);

            } else {
                // ── Full fetch path (no cache available) ────────────
                setLoadingDetail(true);
                const [detail, membersPage] = await Promise.all([
                    apiGet(`/api/groups/${groupId}`),
                    apiGet(`/api/groups/${groupId}/groupMemberships?page=0&size=100`),
                ]);
                setGroupDetail(detail);
                setMembers(membersPage?.content ?? []);
                membersLoadedForGroupRef.current = groupId;

                // Cache the fresh detail
                if (encryptionKey) {
                    const encrypted = await encryptForCache(encryptionKey, detail);
                    storageSetRaw(groupDetailKey(user.id, groupId), encrypted);
                }
                storageSetJson(groupLastSeenKey(user.id, groupId), new Date().toISOString());
            }
        } catch (err) {
            if (err?.status === 403 || err?.status === 404) {
                // User lost access to this group — clean up and redirect
                storageRemove(groupDetailKey(user.id, groupId));
                storageRemove(groupLastSeenKey(user.id, groupId));
                setGroupDetail(null);
                setMembers([]);
                membersLoadedForGroupRef.current = null;

                await loadGroupsList();
                window.dispatchEvent(
                    new CustomEvent("group-access-lost", {
                        detail: { groupId, message: err?.message || "You no longer have access to this group." },
                    })
                );
            } else if (!cachedDetail) {
                // Network error with no cache to fall back on
                setGroupDetail(null);
                setMembers([]);
            }
        } finally {
            setLoadingDetail(false);
        }
    }


    // =================================================================
    // Group actions — exposed through context for pages/components
    // =================================================================

    const selectGroup = useCallback((group) => {
        detailLoadedForGroupRef.current = null;
        setActiveGroup(group);
    }, []);

    /** Encrypts and persists the groups list to LS (fire-and-forget). */
    const persistGroupsListToStorage = useCallback(async (list) => {
        const encryptionKey = cacheKeyRef.current;
        if (encryptionKey && user?.id) {
            const encrypted = await encryptForCache(encryptionKey, list);
            storageSetRaw(groupsListKey(user.id), encrypted);
        }
    }, [user]);

    const addGroup = useCallback((newGroup) => {
        setGroups(prev => {
            const updated = [...prev, newGroup];
            persistGroupsListToStorage(updated);
            return updated;
        });
        setActiveGroup(newGroup);
    }, [user, persistGroupsListToStorage]);

    const updateGroup = useCallback((updatedGroup) => {
        setGroups(prev => {
            const updated = prev.map(g => g.id === updatedGroup.id ? updatedGroup : g);
            persistGroupsListToStorage(updated);
            return updated;
        });
        setActiveGroup(updatedGroup);

        // Mark the ref with this group's ID so the activeGroup effect skips —
        // we're already refreshing detail explicitly right here.
        detailLoadedForGroupRef.current = updatedGroup.id;
        loadOrRefreshGroupDetail(updatedGroup.id);
    }, [user, persistGroupsListToStorage]);

    const refreshActiveGroup = useCallback(() => {
        if (activeGroup) {
            detailLoadedForGroupRef.current = null;
            loadOrRefreshGroupDetail(activeGroup.id);
        }
    }, [activeGroup]);

    const removeGroupFromState = useCallback((groupId) => {
        setGroups(prev => {
            const remaining = prev.filter(g => g.id !== groupId);
            persistGroupsListToStorage(remaining);

            if (activeGroup?.id === groupId) {
                setActiveGroup(remaining.length ? remaining[0] : null);
            }
            return remaining;
        });

        // Clean up this group's cached detail and timestamp
        if (user?.id) {
            storageRemove(groupDetailKey(user.id, groupId));
            storageRemove(groupLastSeenKey(user.id, groupId));
        }
    }, [user, activeGroup, persistGroupsListToStorage]);

    const reloadGroups = useCallback(() => {
        if (user) loadGroupsList();
    }, [user]);

    /** Optimistically marks group events as seen for the current user. */
    const markGroupEventsSeen = useCallback(() => {
        if (!user) return;
        setMembers(prev =>
            prev.map(m =>
                m.user?.id === user.id
                    ? { ...m, lastSeenGroupEvents: new Date().toISOString() }
                    : m
            )
        );
    }, [user]);


    // =================================================================
    // Polling — 3-tier idle degradation
    //
    // Active:    poll every 30s (60s after a long session of 30+ min)
    // Idle 10m:  slow down to 60s
    // Idle 15m:  stop polling entirely, show "stale" banner
    //
    // When the user returns from idle, we do a full detail reload
    // before resuming the poll cycle.
    // =================================================================

    // Timing constants
    const POLL_ACTIVE_MS       = 30_000;       // 30s between polls while active
    const POLL_LONG_SESSION_MS = 60_000;       // 60s if session > 30 min
    const SLOW_DOWN_AFTER_MS   = 10 * 60_000;  // start slowing after 10 min idle
    const POLL_SLOW_MS         = 60_000;       // 60s in slow tier
    const STOP_AFTER_MS        = 15 * 60_000;  // stop completely after 15 min idle
    const LONG_SESSION_MS      = 30 * 60_000;  // "long session" = 30 min

    const [isStale, setIsStale]  = useState(false);

    const pollTimerRef    = useRef(null);
    const lastActivityRef = useRef(Date.now());
    const sessionStartRef = useRef(Date.now());

    /** Decides how often to poll based on idle time and session length. */
    function choosePollInterval() {
        const idleMs = Date.now() - lastActivityRef.current;
        if (idleMs >= STOP_AFTER_MS)     return null;           // stop
        if (idleMs >= SLOW_DOWN_AFTER_MS) return POLL_SLOW_MS;  // slow tier

        const sessionAge = Date.now() - sessionStartRef.current;
        return sessionAge > LONG_SESSION_MS ? POLL_LONG_SESSION_MS : POLL_ACTIVE_MS;
    }

    const schedulePoll = useCallback(() => {
        if (pollTimerRef.current) clearTimeout(pollTimerRef.current);

        const interval = choosePollInterval();
        if (interval === null) {
            setIsStale(true);
            return;
        }

        pollTimerRef.current = setTimeout(async () => {
            // Double-check idle time inside the timeout (user might have gone idle
            // between scheduling and firing)
            const idleMs = Date.now() - lastActivityRef.current;
            if (idleMs >= STOP_AFTER_MS) {
                setIsStale(true);
                return;
            }

            if (activeGroup) {
                const lastSeen = storageGetJson(groupLastSeenKey(user.id, activeGroup.id));
                if (lastSeen) {
                    // Lightweight check: has anything changed since lastSeen?
                    // Server returns 204 (no) or 409 (yes)
                    try {
                        await apiGet(
                            `/api/groups/${activeGroup.id}/has-changed?lastSeen=${encodeURIComponent(lastSeen)}`
                        );
                    } catch (err) {
                        if (err?.status === 409) {
                            loadOrRefreshGroupDetail(activeGroup.id);
                        }
                    }
                } else {
                    // No lastSeen stored — do a full detail load
                    loadOrRefreshGroupDetail(activeGroup.id);
                }
            }

            schedulePoll();
        }, interval);
    }, [activeGroup, user]);

    // Throttle activity events: ignore clicks/scrolls that arrive within 5s
    // of the last poll reschedule to avoid hammering clearTimeout/setTimeout.
    const lastPollRescheduleRef   = useRef(0);
    const ACTIVITY_THROTTLE_MS   = 5_000;

    const onUserActivity = useCallback(() => {
        lastActivityRef.current = Date.now();

        // Coming back from stale/idle — reload detail and restart polling
        if (isStale) {
            setIsStale(false);
            if (activeGroup) {
                detailLoadedForGroupRef.current = null;
                loadOrRefreshGroupDetail(activeGroup.id);
            }
            lastPollRescheduleRef.current = Date.now();
            schedulePoll();
            return;
        }

        // Normal activity — only reschedule if enough time passed
        if (Date.now() - lastPollRescheduleRef.current >= ACTIVITY_THROTTLE_MS) {
            lastPollRescheduleRef.current = Date.now();
            schedulePoll();
        }
    }, [activeGroup, isStale, schedulePoll]);

    /** Explicit refresh button — always reloads detail + resets poll cycle. */
    const manualRefresh = useCallback(() => {
        lastActivityRef.current = Date.now();
        setIsStale(false);
        if (activeGroup) {
            detailLoadedForGroupRef.current = null;
            loadOrRefreshGroupDetail(activeGroup.id);
        }
        schedulePoll();
    }, [activeGroup, schedulePoll]);


    // =================================================================
    // Effect: start/stop poll cycle when active group or user changes
    // =================================================================

    useEffect(() => {
        if (!activeGroup || !user) {
            if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
            return;
        }
        lastActivityRef.current = Date.now();
        setIsStale(false);
        schedulePoll();
        return () => { if (pollTimerRef.current) clearTimeout(pollTimerRef.current); };
    }, [activeGroup, user, schedulePoll]);


    // =================================================================
    // Effect: listen for user activity (click, keydown, scroll, pointer)
    // =================================================================

    useEffect(() => {
        if (!activeGroup || !user) return;
        const events = ["click", "keydown", "scroll", "pointerdown"];
        events.forEach(ev => window.addEventListener(ev, onUserActivity, { passive: true }));
        return () => events.forEach(ev => window.removeEventListener(ev, onUserActivity));
    }, [activeGroup, user, onUserActivity]);


    // =================================================================
    // Context value
    // =================================================================

    return (
        <GroupContext.Provider value={{
            groups,
            activeGroup,
            groupDetail,
            members,
            myRole,
            loadingGroups,
            loadingDetail,
            isStale,
            selectGroup,
            addGroup,
            updateGroup,
            refreshActiveGroup,
            removeGroupFromState,
            reloadGroups,
            manualRefresh,
            markGroupEventsSeen,
        }}>
            {children}
        </GroupContext.Provider>
    );
}
