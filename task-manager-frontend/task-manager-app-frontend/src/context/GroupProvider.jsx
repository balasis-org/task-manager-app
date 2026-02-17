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

    const [groups, setGroups]           = useState([]);
    const [activeGroup, setActiveGroup] = useState(null);
    const [groupDetail, setGroupDetail] = useState(null);
    const [members, setMembers]         = useState([]);
    const [loadingGroups, setLoadingGroups] = useState(true);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [myRole, setMyRole]           = useState(null);

    // --- when user changes (login / logout / switch) ---
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

        // grab the cache key the backend gave us through /users/me
        cacheKeyRef.current = user.cacheKey || null;
        loadGroups();
    }, [user]);

    // --- when active group changes, load or refresh its detail ---
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

    // derive current user's role from members list
    useEffect(() => {
        if (user && members.length > 0) {
            const mine = members.find(m => m.user?.id === user.id);
            setMyRole(mine?.role ?? null);
        } else {
            setMyRole(null);
        }
    }, [members, user]);


    // ── lightweight group list (not sensitive — stored plain) ──
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


    // ── full group detail — encrypted in localStorage ──
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
        } catch {
            if (!cachedDetail) {
                setGroupDetail(null);
                setMembers([]);
            }
        } finally {
            setLoadingDetail(false);
        }
    }


    // ── actions exposed to the rest of the app ──

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
        // throw away old encrypted cache for this group so we do a full load
        if (user?.id) {
            lsRemove(kDetail(user.id, updatedGroup.id));
            lsRemove(kLastSeen(user.id, updatedGroup.id));
        }
        loadOrRefreshDetail(updatedGroup.id);
    }, [user]);

    const refreshActiveGroup = useCallback(() => {
        if (activeGroup) loadOrRefreshDetail(activeGroup.id);
    }, [activeGroup]);

    // ── 5-minute polling, reset on user activity ──
    const POLL_INTERVAL = 5 * 60 * 1000; // 5 minutes
    const pollTimer = useRef(null);

    const resetPollTimer = useCallback(() => {
        if (pollTimer.current) clearTimeout(pollTimer.current);
        pollTimer.current = setTimeout(() => {
            if (activeGroup) loadOrRefreshDetail(activeGroup.id);
            // after the refresh fires, start the timer again
            resetPollTimer();
        }, POLL_INTERVAL);
    }, [activeGroup]);

    // start / restart polling when activeGroup changes
    useEffect(() => {
        if (!activeGroup || !user) {
            if (pollTimer.current) clearTimeout(pollTimer.current);
            return;
        }
        resetPollTimer();
        return () => { if (pollTimer.current) clearTimeout(pollTimer.current); };
    }, [activeGroup, user, resetPollTimer]);

    // reset timer on user activity (clicks, key presses, scrolls)
    useEffect(() => {
        if (!activeGroup || !user) return;
        const handler = () => resetPollTimer();
        const events = ["click", "keydown", "scroll", "pointerdown"];
        events.forEach((ev) => window.addEventListener(ev, handler, { passive: true }));
        return () => events.forEach((ev) => window.removeEventListener(ev, handler));
    }, [activeGroup, user, resetPollTimer]);

    return (
        <GroupContext.Provider value={{
            groups,
            activeGroup,
            groupDetail,
            members,
            myRole,
            loadingGroups,
            loadingDetail,
            selectGroup,
            addGroup,
            updateGroup,
            refreshActiveGroup,
        }}>
            {children}
        </GroupContext.Provider>
    );
}
