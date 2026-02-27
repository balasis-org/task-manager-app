import { useState, useEffect, useCallback, useContext, useRef } from "react";
import { GroupContext } from "./GroupContext";
import { AuthContext } from "./AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import { encryptForCache, decryptFromCache, cacheMatchesKey } from "@assets/js/cacheCrypto.js";

const LS_PREFIX = "tm_u";

function lsGetRaw(key) { return localStorage.getItem(key); }
function lsSetRaw(key, val) { localStorage.setItem(key, val); }
function lsGet(key)    { try { return JSON.parse(localStorage.getItem(key)); } catch { return null; } }
function lsSet(key, v) { localStorage.setItem(key, JSON.stringify(v)); }
function lsRemove(key) { localStorage.removeItem(key); }

function pfx(userId)   { return userId ? `${LS_PREFIX}${userId}_` : ""; }

function kGroups(uid)             { return `${pfx(uid)}groups`; }
function kActiveId(uid)           { return `${pfx(uid)}active_group`; }
function kDetail(uid, gid)        { return `${pfx(uid)}gd_${gid}`; }
function kLastSeen(uid, gid)      { return `${pfx(uid)}ls_${gid}`; }

function mergeRefresh(cached, delta) {
    if (!delta.c) return cached;
    const merged = { ...cached };

    if (delta.n   !== undefined && delta.n   !== null) merged.n   = delta.n;
    if (delta.d   !== undefined && delta.d   !== null) merged.d   = delta.d;
    if (delta.an  !== undefined)   merged.an  = delta.an;
    if (delta.diu !== undefined)   merged.diu = delta.diu;
    if (delta.iu  !== undefined)   merged.iu  = delta.iu;
    if (delta.aen !== undefined && delta.aen !== null)
        merged.aen = delta.aen;
    if (delta.lged !== undefined)
         merged.lged = delta.lged;

    let tasks = [...(merged.tp ?? [])];
    if (delta.dti?.length) {
        const gone = new Set(delta.dti);
        tasks = tasks.filter(t => !gone.has(t.i));
    }

    if (delta.ct?.length) {
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

    const cacheKeyRef = useRef(null);

    cacheKeyRef.current = user?.cacheKey || null;

    const [groups, setGroups]           = useState([]);
    const [activeGroup, setActiveGroup] = useState(null);
    const [groupDetail, setGroupDetail] = useState(null);
    const [members, setMembers]         = useState([]);
    const [loadingGroups, setLoadingGroups] = useState(true);
    const [loadingDetail, setLoadingDetail] = useState(false);
    const [myRole, setMyRole]           = useState(null);

    const membersLoadedForGroupRef = useRef(null);

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

    useEffect(() => {
        if (user && members.length > 0) {
            const mine = members.find(m => m.user?.id === user.id);
            setMyRole(mine?.role ?? null);
        } else {
            setMyRole(null);
        }
    }, [members, user]);

    async function loadGroups() {
        setLoadingGroups(true);
        try {
            const fresh = await apiGet("/api/groups");
            const list = Array.isArray(fresh) ? fresh : [];
            setGroups(list);
            lsSet(kGroups(user.id), list);

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

    async function loadOrRefreshDetail(groupId) {
        const ck = cacheKeyRef.current;

        let cachedDetail = null;
        const storedBlob = lsGetRaw(kDetail(user.id, groupId));
        const cachedLastSeen = lsGet(kLastSeen(user.id, groupId));

        if (ck && storedBlob && cacheMatchesKey(ck, storedBlob)) {

            cachedDetail = await decryptFromCache(ck, storedBlob);
        }

        if (cachedDetail && cachedDetail.taskPreviews !== undefined && cachedDetail.tp === undefined) {
            cachedDetail = null;
            lsRemove(kDetail(user.id, groupId));
            lsRemove(kLastSeen(user.id, groupId));
        }
        if (cachedDetail) setGroupDetail(cachedDetail);

        try {
            if (cachedDetail && cachedLastSeen) {

                const delta = await apiGet(
                    `/api/groups/${groupId}/refresh?lastSeen=${encodeURIComponent(cachedLastSeen)}`
                );

                if (delta.mc || membersLoadedForGroupRef.current !== groupId) {
                    const membersPage = await apiGet(
                        `/api/groups/${groupId}/groupMemberships?page=0&size=100`
                    );
                    setMembers(membersPage?.content ?? []);
                    membersLoadedForGroupRef.current = groupId;
                }

                const final_ = delta.c ? mergeRefresh(cachedDetail, delta) : cachedDetail;
                setGroupDetail(final_);

                if (ck) {
                    const enc = await encryptForCache(ck, final_);
                    lsSetRaw(kDetail(user.id, groupId), enc);
                }
                lsSet(kLastSeen(user.id, groupId), delta.sn);
            } else {

                setLoadingDetail(true);
                const [detail, membersPage] = await Promise.all([
                    apiGet(`/api/groups/${groupId}`),
                    apiGet(`/api/groups/${groupId}/groupMemberships?page=0&size=100`),
                ]);
                setGroupDetail(detail);
                setMembers(membersPage?.content ?? []);
                membersLoadedForGroupRef.current = groupId;

                if (ck) {
                    const enc = await encryptForCache(ck, detail);
                    lsSetRaw(kDetail(user.id, groupId), enc);
                }
                lsSet(kLastSeen(user.id, groupId), new Date().toISOString());
            }
        } catch (err) {

            if (err?.status === 403 || err?.status === 404) {

                lsRemove(kDetail(user.id, groupId));
                lsRemove(kLastSeen(user.id, groupId));
                setGroupDetail(null);
                setMembers([]);
                membersLoadedForGroupRef.current = null;

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

        loadOrRefreshDetail(updatedGroup.id);
    }, [user]);

    const refreshActiveGroup = useCallback(() => {
        if (activeGroup) loadOrRefreshDetail(activeGroup.id);
    }, [activeGroup]);

    const removeGroupFromState = useCallback((groupId) => {
        setGroups(prev => {
            const next = prev.filter(g => g.id !== groupId);
            if (user?.id) lsSet(kGroups(user.id), next);

            if (activeGroup?.id === groupId) {
                const fallback = next.length ? next[0] : null;
                setActiveGroup(fallback);
            }
            return next;
        });

        if (user?.id) {
            lsRemove(kDetail(user.id, groupId));
            lsRemove(kLastSeen(user.id, groupId));
        }
    }, [user, activeGroup]);

    const reloadGroups = useCallback(() => {
        if (user) loadGroups();
    }, [user]);

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

    const TIER1_MS         = 30_000;
    const TIER1_LONG_MS    = 60_000;
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
        if (idle >= TIER2_UNTIL) return null;
        if (idle >= TIER1_UNTIL) return TIER2_MS;

        const sessionAge = Date.now() - sessionStart.current;
        return sessionAge > LONG_SESSION_MS ? TIER1_LONG_MS : TIER1_MS;
    }

    const schedulePoll = useCallback(() => {
        if (pollTimer.current) clearTimeout(pollTimer.current);

        const interval = getPollInterval();
        if (interval === null) {

            setIsStale(true);
            return;
        }

        pollTimer.current = setTimeout(() => {

            const idle = Date.now() - lastActivity.current;
            if (idle >= TIER2_UNTIL) {
                setIsStale(true);
                return;
            }

            if (activeGroup) loadOrRefreshDetail(activeGroup.id);

            schedulePoll();
        }, interval);
    }, [activeGroup]);

    const onUserActivity = useCallback(() => {
        lastActivity.current = Date.now();
        if (isStale) {
            setIsStale(false);

            if (activeGroup) loadOrRefreshDetail(activeGroup.id);
        }
        schedulePoll();
    }, [activeGroup, isStale, schedulePoll]);

    const manualRefresh = useCallback(() => {
        lastActivity.current = Date.now();
        setIsStale(false);
        if (activeGroup) loadOrRefreshDetail(activeGroup.id);
        schedulePoll();
    }, [activeGroup, schedulePoll]);

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
