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
function mergeRefresh(cached, delta) {
    if (!delta.changed) return cached;
    const merged = { ...cached };

    if (delta.name        !== undefined && delta.name        !== null) merged.name        = delta.name;
    if (delta.description !== undefined && delta.description !== null) merged.description = delta.description;
    if (delta.announcement !== undefined)   merged.announcement = delta.announcement;
    if (delta.defaultImgUrl !== undefined)  merged.defaultImgUrl = delta.defaultImgUrl;
    if (delta.imgUrl       !== undefined)   merged.imgUrl       = delta.imgUrl;
    if (delta.allowEmailNotification !== undefined && delta.allowEmailNotification !== null)
        merged.allowEmailNotification = delta.allowEmailNotification;
    if (delta.lastGroupEventDate !== undefined)
         merged.lastGroupEventDate = delta.lastGroupEventDate;

    // remove deleted tasks
    let tasks = [...(merged.taskPreviews ?? [])];
    if (delta.deletedTaskIds?.length) {
        const gone = new Set(delta.deletedTaskIds);
        tasks = tasks.filter(t => !gone.has(t.id));
    }

    // upsert changed / new tasks
    if (delta.changedTasks?.length) {
        const map = new Map(delta.changedTasks.map(t => [t.id, t]));
        tasks = tasks.map(t => map.has(t.id) ? map.get(t.id) : t);
        const existing = new Set(tasks.map(t => t.id));
        for (const t of delta.changedTasks) {
            if (!existing.has(t.id)) tasks.push(t);
        }
    }

    merged.taskPreviews = tasks;
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
        if (cachedDetail) setGroupDetail(cachedDetail);

        try {
            const membersPromise = apiGet(`/api/groups/${groupId}/groupMemberships?page=0&size=100`);

            if (cachedDetail && cachedLastSeen) {
                // delta refresh — small request
                const [delta, membersPage] = await Promise.all([
                    apiGet(`/api/groups/${groupId}/refresh?lastSeen=${encodeURIComponent(cachedLastSeen)}`),
                    membersPromise,
                ]);
                setMembers(membersPage?.content ?? []);

                const final_ = delta.changed ? mergeRefresh(cachedDetail, delta) : cachedDetail;
                setGroupDetail(final_);

                // encrypt and save back
                if (ck) {
                    const enc = await encryptForCache(ck, final_);
                    lsSetRaw(kDetail(user.id, groupId), enc);
                }
                lsSet(kLastSeen(user.id, groupId), delta.serverNow);
            } else {
                // no usable cache — full load
                const [detail, membersPage] = await Promise.all([
                    apiGet(`/api/groups/${groupId}`),
                    membersPromise,
                ]);
                setGroupDetail(detail);
                setMembers(membersPage?.content ?? []);

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

    // --- Smart polling with progressive backoff ---
    const BASE_POLL_MS   = 60_000;       // 1 minute
    const MAX_POLL_MS    = 5 * 60_000;   // 5 minutes ceiling
    const STALE_AFTER_MS = 20 * 60_000;  // show refresh button after 20 min inactivity
    const [isStale, setIsStale]     = useState(false);

    const pollTimer       = useRef(null);
    const currentInterval = useRef(BASE_POLL_MS);
    const lastActivity    = useRef(Date.now());

    const schedulePoll = useCallback(() => {
        if (pollTimer.current) clearTimeout(pollTimer.current);

        pollTimer.current = setTimeout(() => {
            const idle = Date.now() - lastActivity.current;

            if (idle >= STALE_AFTER_MS) {
                // user has been idle too long — stop polling, mark stale
                setIsStale(true);
                return;
            }

            if (activeGroup) loadOrRefreshDetail(activeGroup.id);

            // double the interval (capped)
            currentInterval.current = Math.min(currentInterval.current * 2, MAX_POLL_MS);
            schedulePoll();
        }, currentInterval.current);
    }, [activeGroup]);

    // reset on user interaction — restart from base interval
    const onUserActivity = useCallback(() => {
        lastActivity.current = Date.now();
        if (isStale) {
            setIsStale(false);
            // user came back — do an immediate refresh
            if (activeGroup) loadOrRefreshDetail(activeGroup.id);
        }
        currentInterval.current = BASE_POLL_MS;
        schedulePoll();
    }, [activeGroup, isStale, schedulePoll]);

    // manual refresh (from the stale banner button)
    const manualRefresh = useCallback(() => {
        lastActivity.current = Date.now();
        setIsStale(false);
        currentInterval.current = BASE_POLL_MS;
        if (activeGroup) loadOrRefreshDetail(activeGroup.id);
        schedulePoll();
    }, [activeGroup, schedulePoll]);

    // kick off / restart polling when group changes
    useEffect(() => {
        if (!activeGroup || !user) {
            if (pollTimer.current) clearTimeout(pollTimer.current);
            return;
        }
        currentInterval.current = BASE_POLL_MS;
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
        }}>
            {children}
        </GroupContext.Provider>
    );
}
