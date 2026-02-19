import { useState, useEffect, useCallback, useContext, useRef } from "react";
import { GroupContext } from "./GroupContext";
import { AuthContext } from "./AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import { encryptForCache, decryptFromCache, cacheMatchesKey } from "@assets/js/cacheCrypto.js";

// localStorage is per origin, not per user.
// we namespace every key by userId so accounts dont bleed into each other.
const LS_PREFIX = "tm_u";

function lsGetRaw(key) { return localStorage.getItem(key); }
function lsSetRaw(key, val) { localStorage.setItem(key, val); }
function lsGet(key)    { try { return JSON.parse(localStorage.getItem(key)); } catch { return null; } }
function lsSet(key, v) { localStorage.setItem(key, JSON.stringify(v)); }
function lsRemove(key) { localStorage.removeItem(key); }

function pfx(userId)   { return userId ? `${LS_PREFIX}${userId}_` : ""; }

function kGroups(uid)             { return `${pfx(uid)}groups`; }
function kActiveId(uid)           { return `${pfx(uid)}active_group`; }
function kDetail(uid, gid)        { return `${pfx(uid)}gd_${gid}`; }       // encrypted blob
function kLastSeen(uid, gid)      { return `${pfx(uid)}ls_${gid}`; }

// merge a delta from the refresh endpoint into an existing detail object
// JSON keys use short names from @JsonProperty (see GroupRefreshDto / GroupWithPreviewDto)
function mergeRefresh(cached, delta) {
    if (!delta.c) return cached;                          // c = changed
    const merged = { ...cached };

    if (delta.n   !== undefined && delta.n   !== null) merged.n   = delta.n;    // name
    if (delta.d   !== undefined && delta.d   !== null) merged.d   = delta.d;    // description
    if (delta.an  !== undefined)   merged.an  = delta.an;                       // announcement
    if (delta.diu !== undefined)   merged.diu = delta.diu;                      // defaultImgUrl
    if (delta.iu  !== undefined)   merged.iu  = delta.iu;                       // imgUrl
    if (delta.aen !== undefined && delta.aen !== null)
        merged.aen = delta.aen;                                                 // allowEmailNotification
    if (delta.lged !== undefined)
         merged.lged = delta.lged;                                              // lastGroupEventDate

    // remove deleted tasks
    let tasks = [...(merged.tp ?? [])];                                         // tp = taskPreviews
    if (delta.dti?.length) {                                                    // dti = deletedTaskIds
        const gone = new Set(delta.dti);
        tasks = tasks.filter(t => !gone.has(t.i));                              // i = id
    }

    // upsert changed / new tasks
    if (delta.ct?.length) {                                                     // ct = changedTasks
        const map = new Map(delta.ct.map(t => [t.i, t]));
        tasks = tasks.map(t => map.has(t.i) ? map.get(t.i) : t);
        const existing = new Set(tasks.map(t => t.i));
        for (const t of delta.ct) {
            if (!existing.has(t.i)) tasks.push(t);
        }
    }

    merged.tp = tasks;
    return merged;
}


export default function GroupProvider({ children }) {
    const { user } = useContext(AuthContext);

    // the encryption key — lives ONLY in memory, never written to storage.
    // we get it fresh from /users/me every page load / refresh.
    const cacheKeyRef = useRef(null);

    // Keep cache key in sync whenever user object updates (e.g. profile refresh)
    cacheKeyRef.current = user?.cacheKey || null;

    const [groups, setGroups]           = useState([]);
    const [activeGroup, setActiveGroup] = useState(null);
    const [groupDetail, setGroupDetail] = useState(null);
    const [members, setMembers]         = useState([]);
    const [loadingGroups, setLoadingGroups] = useState(true);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [myRole, setMyRole]           = useState(null);

    // tracks which group we've already loaded members for this session
    const membersLoadedForGroupRef = useRef(null);

    // user changed? reload everything
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

        loadGroups();
    }, [user?.id]);

    // active group switched -> load/refresh detail
    useEffect(() => {
        if (activeGroup && user) {
            lsSet(kActiveId(user.id), activeGroup.id);
            loadOrRefreshDetail(activeGroup.id);
        } else {
            setGroupDetail(null);
            setMembers([]);
            setMyRole(null);
        }
    }, [activeGroup]);

    // figure out my role from the members list
    useEffect(() => {
        if (user && members.length > 0) {
            const mine = members.find(m => m.user?.id === user.id);
            setMyRole(mine?.role ?? null);
        } else {
            setMyRole(null);
        }
    }, [members, user]);


    // groups list - stored as plain json, nothing sensitive
    async function loadGroups() {
        setLoadingGroups(true);
        try {
            const fresh = await apiGet("/api/groups");
            const list = Array.isArray(fresh) ? fresh : [];
            setGroups(list);
            lsSet(kGroups(user.id), list);

            // restore previously selected group, or pick the first one
            const savedId = lsGet(kActiveId(user.id));
            const match = list.find(g => g.id === savedId);
            if (match) setActiveGroup(match);
            else if (list.length) setActiveGroup(list[0]);
            else setActiveGroup(null);
        } catch {
            setGroups([]);
        } finally {
            setLoadingGroups(false);
        }
    }


    // full detail - encrypted in localstorage
    async function loadOrRefreshDetail(groupId) {
        setLoadingDetail(true);
        const ck = cacheKeyRef.current;

        // try to read + decrypt cached detail
        let cachedDetail = null;
        const storedBlob = lsGetRaw(kDetail(user.id, groupId));
        const cachedLastSeen = lsGet(kLastSeen(user.id, groupId));

        if (ck && storedBlob && cacheMatchesKey(ck, storedBlob)) {
            // key matches — worth trying to decrypt
            cachedDetail = await decryptFromCache(ck, storedBlob);
        }

        // if we got something, show it right away while we refresh in the background
        // migrate old cache format (long key names) → force full reload
        if (cachedDetail && cachedDetail.taskPreviews !== undefined && cachedDetail.tp === undefined) {
            cachedDetail = null;
            lsRemove(kDetail(user.id, groupId));
            lsRemove(kLastSeen(user.id, groupId));
        }
        if (cachedDetail) setGroupDetail(cachedDetail);

        try {
            if (cachedDetail && cachedLastSeen) {
                // delta refresh — small request
                const delta = await apiGet(
                    `/api/groups/${groupId}/refresh?lastSeen=${encodeURIComponent(cachedLastSeen)}`
                );

                // fetch members when they changed OR when not yet loaded for this group
                if (delta.mc || membersLoadedForGroupRef.current !== groupId) {     // mc = membersChanged
                    const membersPage = await apiGet(
                        `/api/groups/${groupId}/groupMemberships?page=0&size=100`
                    );
                    setMembers(membersPage?.content ?? []);
                    membersLoadedForGroupRef.current = groupId;
                }

                const final_ = delta.c ? mergeRefresh(cachedDetail, delta) : cachedDetail;  // c = changed
                setGroupDetail(final_);

                // encrypt and save back
                if (ck) {
                    const enc = await encryptForCache(ck, final_);
                    lsSetRaw(kDetail(user.id, groupId), enc);
                }
                lsSet(kLastSeen(user.id, groupId), delta.sn);    // sn = serverNow
            } else {
                // no usable cache — full load (always fetch members)
                const [detail, membersPage] = await Promise.all([
                    apiGet(`/api/groups/${groupId}`),
                    apiGet(`/api/groups/${groupId}/groupMemberships?page=0&size=100`),
                ]);
                setGroupDetail(detail);
                setMembers(membersPage?.content ?? []);
                membersLoadedForGroupRef.current = groupId;

                // encrypt and save
                if (ck) {
                    const enc = await encryptForCache(ck, detail);
                    lsSetRaw(kDetail(user.id, groupId), enc);
                }
                lsSet(kLastSeen(user.id, groupId), new Date().toISOString());
            }
        } catch (err) {
            // If 403 or 404 the user was likely removed from the group (or group deleted).
            // Auto-recover by reloading the groups list so the stale group disappears.
            if (err?.status === 403 || err?.status === 404) {
                // wipe local cache for this group
                lsRemove(kDetail(user.id, groupId));
                lsRemove(kLastSeen(user.id, groupId));
                setGroupDetail(null);
                setMembers([]);
                membersLoadedForGroupRef.current = null;
                // reload will drop the missing group and auto-select another
                await loadGroups();
                window.dispatchEvent(
                    new CustomEvent("group-access-lost", {
                        detail: { groupId, message: err?.message || "You no longer have access to this group." },
                    })
                );
            } else if (!cachedDetail) {
                setGroupDetail(null);
                setMembers([]);
            }
        } finally {
            setLoadingDetail(false);
        }
    }


    // --- public api ---
    const selectGroup = useCallback((g) => setActiveGroup(g), []);

    const addGroup = useCallback((newGroup) => {
        setGroups(prev => {
            const next = [...prev, newGroup];
            if (user?.id) lsSet(kGroups(user.id), next);
            return next;
        });
        setActiveGroup(newGroup);
    }, [user]);

    const updateGroup = useCallback((updatedGroup) => {
        setGroups(prev => {
            const next = prev.map(g => g.id === updatedGroup.id ? updatedGroup : g);
            if (user?.id) lsSet(kGroups(user.id), next);
            return next;
        });
        setActiveGroup(updatedGroup);
        // wipe old cache so we do a full re-load next time
        if (user?.id) {
            lsRemove(kDetail(user.id, updatedGroup.id));
            lsRemove(kLastSeen(user.id, updatedGroup.id));
        }
        loadOrRefreshDetail(updatedGroup.id);
    }, [user]);

    const refreshActiveGroup = useCallback(() => {
        if (activeGroup) loadOrRefreshDetail(activeGroup.id);
    }, [activeGroup]);

    const removeGroupFromState = useCallback((groupId) => {
        setGroups(prev => {
            const next = prev.filter(g => g.id !== groupId);
            if (user?.id) lsSet(kGroups(user.id), next);
            // If we removed the active group, switch to next available
            if (activeGroup?.id === groupId) {
                const fallback = next.length ? next[0] : null;
                setActiveGroup(fallback);
            }
            return next;
        });
        // clear cached detail
        if (user?.id) {
            lsRemove(kDetail(user.id, groupId));
            lsRemove(kLastSeen(user.id, groupId));
        }
    }, [user, activeGroup]);

    const reloadGroups = useCallback(() => {
        if (user) loadGroups();
    }, [user]);

    // Optimistically update the current user's lastSeenGroupEvents in the local members array
    // so we don't need to re-fetch the whole group detail + members just for this timestamp.
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

    // --- Smart polling with tiered backoff ---
    // Tier 1: 30 s   for the first 10 min of inactivity (1 min if session > 30 min)
    // Tier 2: 1 min  from 10 → 15 min of inactivity
    // Tier 3: STOP   after 15 min (show refresh button only)
    const TIER1_MS         = 30_000;
    const TIER1_LONG_MS    = 60_000;          // after 30 min active session
    const TIER1_UNTIL      = 10 * 60_000;
    const TIER2_MS         = 60_000;
    const TIER2_UNTIL      = 15 * 60_000;
    const LONG_SESSION_MS  = 30 * 60_000;

    const [isStale, setIsStale]     = useState(false);

    const pollTimer       = useRef(null);
    const lastActivity    = useRef(Date.now());
    const sessionStart    = useRef(Date.now());

    function getPollInterval() {
        const idle = Date.now() - lastActivity.current;
        if (idle >= TIER2_UNTIL) return null;            // stop polling
        if (idle >= TIER1_UNTIL) return TIER2_MS;        // 1 min

        // Tier 1 — use longer base after 30 min of session
        const sessionAge = Date.now() - sessionStart.current;
        return sessionAge > LONG_SESSION_MS ? TIER1_LONG_MS : TIER1_MS;
    }

    const schedulePoll = useCallback(() => {
        if (pollTimer.current) clearTimeout(pollTimer.current);

        const interval = getPollInterval();
        if (interval === null) {
            // idle too long — stop polling, show refresh button
            setIsStale(true);
            return;
        }

        pollTimer.current = setTimeout(() => {
            // re-check idle right before firing
            const idle = Date.now() - lastActivity.current;
            if (idle >= TIER2_UNTIL) {
                setIsStale(true);
                return;
            }

            if (activeGroup) loadOrRefreshDetail(activeGroup.id);

            schedulePoll();   // re-schedule at the (possibly new) tier
        }, interval);
    }, [activeGroup]);

    // reset on user interaction — restart from tier 1
    const onUserActivity = useCallback(() => {
        lastActivity.current = Date.now();
        if (isStale) {
            setIsStale(false);
            // user came back — do an immediate refresh
            if (activeGroup) loadOrRefreshDetail(activeGroup.id);
        }
        schedulePoll();
    }, [activeGroup, isStale, schedulePoll]);

    // manual refresh (from the stale banner button)
    const manualRefresh = useCallback(() => {
        lastActivity.current = Date.now();
        setIsStale(false);
        if (activeGroup) loadOrRefreshDetail(activeGroup.id);
        schedulePoll();
    }, [activeGroup, schedulePoll]);

    // kick off / restart polling when group changes
    useEffect(() => {
        if (!activeGroup || !user) {
            if (pollTimer.current) clearTimeout(pollTimer.current);
            return;
        }
        lastActivity.current = Date.now();
        setIsStale(false);
        schedulePoll();
        return () => { if (pollTimer.current) clearTimeout(pollTimer.current); };
    }, [activeGroup, user, schedulePoll]);

    // listen for user-interaction events to reset backoff
    useEffect(() => {
        if (!activeGroup || !user) return;
        const events = ["click", "keydown", "scroll", "pointerdown"];
        events.forEach((ev) => window.addEventListener(ev, onUserActivity, { passive: true }));
        return () => events.forEach((ev) => window.removeEventListener(ev, onUserActivity));
    }, [activeGroup, user, onUserActivity]);

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
