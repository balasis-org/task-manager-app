import { useState, useEffect, useContext, useCallback, useRef } from "react";
import { GroupContext } from "@context/GroupContext";
import { AuthContext } from "@context/AuthContext";
import { useNavigate } from "react-router-dom";
import InviteToGroupPopup from "@components/popups/InviteToGroupPopup";
import GroupSettingsPopup from "@components/popups/GroupSettingsPopup";
import GroupEventsPopup from "@components/popups/GroupEventsPopup";
import NewGroupPopup from "@components/popups/NewGroupPopup";
import NewTaskPopup from "@components/popups/NewTaskPopup";
import DashboardTopBar from "@components/dashboard/DashboardTopBar";
import DashboardEmptyState from "@components/dashboard/DashboardEmptyState";
import DashboardColumnHeaders from "@components/dashboard/DashboardColumnHeaders";
import DashboardTaskSection from "@components/dashboard/DashboardTaskSection";
import Spinner from "@components/Spinner";
import { useToast } from "@context/ToastContext";
import { apiGet } from "@assets/js/apiClient.js";
import { FILTER_EMPTY, isFilterEmpty } from "@components/dashboard/FilterPanel";
import { FiRefreshCw } from "react-icons/fi";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Dashboard.css";

const TASK_STATES = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];
const STATE_LABELS = {TODO: "TODO",IN_PROGRESS: "In progress",TO_BE_REVIEWED: "To be reviewed",DONE: "Done"};

const COL_DEFAULTS = [1, 130, 90, 120, 90, 60];
const COL_MIN      = [120, 70, 60, 80, 60, 40];

function visibleCols(width) {
    if (width <= 480) return [0, 2, 5];
    if (width <= 768) return [0, 2, 3, 4, 5];
    if (width <= 768) return [0, 2, 4, 5];
    return [0, 1, 2, 3, 4, 5];
}

function gridCols(w, vis) {
    return vis.map((ci) => (ci === 0 ? "minmax(0,1fr)" : w[ci] + "px")).join(" ");
}

export default function Dashboard() {
    const {
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
        manualRefresh,
        markGroupEventsSeen,
        presenceUserIds,
    } = useContext(GroupContext);
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const showToast = useToast();

    usePageTitle("Dashboard");

    useEffect(() => {
        if (activeGroup) refreshActiveGroup();
    }, []);

    useEffect(() => {
        const handler = (e) => {
            const msg = e.detail?.message || "You no longer have access to this group.";
            showToast(msg, "error");
        };
        window.addEventListener("group-access-lost", handler);
        return () => window.removeEventListener("group-access-lost", handler);
    }, [showToast]);

    const [vpWidth, setVpWidth] = useState(() => window.innerWidth);
    useEffect(() => {
        const onResize = () => setVpWidth(window.innerWidth);
        window.addEventListener("resize", onResize);
        return () => window.removeEventListener("resize", onResize);
    }, []);
    const visCols = visibleCols(vpWidth);

    const [colWidths, setColWidths] = useState(COL_DEFAULTS);

    const onPointerDown = useCallback((leftIdx, e) => {
        e.preventDefault();
        const rightIdx = leftIdx + 1;
        if (rightIdx >= COL_DEFAULTS.length) return;
        const startX = e.clientX;
        const startL = colWidths[leftIdx];
        const startR = colWidths[rightIdx];

        function onMove(ev) {
            const dx = ev.clientX - startX;
            setColWidths((prev) => {
                const next = [...prev];
                const newL = Math.max(COL_MIN[leftIdx], startL + dx);
                const newR = Math.max(COL_MIN[rightIdx], startR - dx);

                if (newL >= COL_MIN[leftIdx] && newR >= COL_MIN[rightIdx]) {
                    next[leftIdx] = newL;
                    next[rightIdx] = newR;
                }
                return next;
            });
        }
        function onUp() {
            window.removeEventListener("pointermove", onMove);
            window.removeEventListener("pointerup", onUp);
        }
        window.addEventListener("pointermove", onMove);
        window.addEventListener("pointerup", onUp);
    }, [colWidths]);

    const onTitleHandleDown = useCallback((nextColIdx, e) => {
        e.preventDefault();
        const startX = e.clientX;
        const startW = colWidths[nextColIdx];

        function onMove(ev) {
            const dx = ev.clientX - startX;
            setColWidths((prev) => {
                const next = [...prev];
                next[nextColIdx] = Math.max(COL_MIN[nextColIdx], startW - dx);
                return next;
            });
        }
        function onUp() {
            window.removeEventListener("pointermove", onMove);
            window.removeEventListener("pointerup", onUp);
        }
        window.addEventListener("pointermove", onMove);
        window.addEventListener("pointerup", onUp);
    }, [colWidths]);

    const [collapsedSections, setCollapsedSections] = useState({});
    const [topBarOpen, setTopBarOpen] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");

    const [showNewGroup, setShowNewGroup] = useState(false);
    const [showInvite, setShowInvite] = useState(false);
    const [showGroupSettings, setShowGroupSettings] = useState(false);
    const [showGroupEvents, setShowGroupEvents] = useState(false);
    const [showNewTask, setShowNewTask] = useState(false);
    const [newTaskState, setNewTaskState] = useState("TODO");

    const [filters, setFilters] = useState({ ...FILTER_EMPTY });
    const [filterIds, setFilterIds] = useState(null);
    const [isFilterApplied, setIsFilterApplied] = useState(false);
    const filterVersionRef = useRef(0);

    function buildFilterParams(f) {
        const p = new URLSearchParams();
        if (f.creatorId)     p.set("creatorId", f.creatorId);
        if (f.reviewerId)    p.set("reviewerId", f.reviewerId);
        if (f.assigneeId)    p.set("assigneeId", f.assigneeId);
        if (f.priorityMin)   p.set("priorityMin", f.priorityMin);
        if (f.priorityMax)   p.set("priorityMax", f.priorityMax);
        if (f.taskState)     p.set("taskState", f.taskState);
        if (f.hasFiles)      p.set("hasFiles", f.hasFiles);
        if (f.dueDateBefore) {
            p.set("dueDateBefore", new Date(f.dueDateBefore + "T23:59:59").toISOString());
        }
        return p.toString();
    }

    async function fetchFilterIds(groupId, criteria) {
        const version = ++filterVersionRef.current;
        try {
            const qs = buildFilterParams(criteria);
            const ids = await apiGet(`/api/groups/${groupId}/tasks/filter-ids?${qs}`);
            if (filterVersionRef.current === version) {
                setFilterIds(new Set(ids));
            }
        } catch {
            if (filterVersionRef.current === version) setFilterIds(null);
        }
    }

    const handleApplyFilters = useCallback(() => {
        if (!activeGroup?.id || isFilterEmpty(filters)) return;
        setIsFilterApplied(true);
        fetchFilterIds(activeGroup.id, filters);
    }, [activeGroup?.id, filters]);

    const handleEditFilters = useCallback(() => {
        setIsFilterApplied(false);
    }, []);

    const handleClearFilters = useCallback(() => {
        setFilters({ ...FILTER_EMPTY });
        setFilterIds(null);
        setIsFilterApplied(false);
    }, []);

    const filtersRef = useRef(filters);
    filtersRef.current = filters;
    const isFilterAppliedRef = useRef(isFilterApplied);
    isFilterAppliedRef.current = isFilterApplied;
    useEffect(() => {
        if (isFilterAppliedRef.current && !isFilterEmpty(filtersRef.current) && activeGroup?.id && groupDetail) {
            fetchFilterIds(activeGroup.id, filtersRef.current);
        }
    }, [groupDetail]);

    useEffect(() => {
        setFilters({ ...FILTER_EMPTY });
        setFilterIds(null);
        setIsFilterApplied(false);
    }, [activeGroup?.id]);

    function handleGroupCreated(newGroup) {
        setShowNewGroup(false);
        addGroup(newGroup);
    }

    function handleGroupUpdated(updatedGroup) {

        updateGroup(updatedGroup);
    }

    function handleLeaveGroup(action) {
        if (action === "left" || action === "deleted") {
            removeGroupFromState(activeGroup?.id);
        } else {
            refreshActiveGroup();
        }
    }

    function toggleSection(state) {
        setCollapsedSections((prev) => ({
            ...prev,
            [state]: !prev[state],
        }));
    }

    const canManageTasks = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";

    const showDeleteColumn = canManageTasks && groupDetail?.tp?.some(t => t.dl !== false);

    function handleOpenNewTask(e, state) {
        e.stopPropagation();
        if (!activeGroup?.id) return;
        setNewTaskState(state);
        setShowNewTask(true);
    }

    function handleTaskCreated(created) {
        setShowNewTask(false);
        showToast("Task created!", "success");
        navigate(`/group/${activeGroup.id}/task/${created.id}`);
    }

    const tasksByState = {};
    for (const s of TASK_STATES) tasksByState[s] = [];
    if (groupDetail?.tp) {
        const q = searchQuery.toLowerCase().trim();
        for (const task of groupDetail.tp) {

            if (filterIds && !filterIds.has(task.i)) continue;

            if (q && !task.t?.toLowerCase().includes(q)) continue;
            const state = task.ts || "TODO";
            if (tasksByState[state]) {
                tasksByState[state].push(task);
            }
        }
    }

    if (!loadingGroups && groups.length === 0) {
        return (
            <DashboardEmptyState
                showNewGroup={showNewGroup}
                onOpenNewGroup={() => setShowNewGroup(true)}
                onCloseNewGroup={() => setShowNewGroup(false)}
                onGroupCreated={handleGroupCreated}
            />
        );
    }

    return (
        <div className="dashboard">
            <DashboardTopBar
                groups={groups}
                activeGroup={activeGroup}
                members={members}
                myRole={myRole}
                open={topBarOpen}
                onToggle={() => setTopBarOpen((v) => !v)}
                onSelectGroup={(g) => selectGroup(g)}
                onOpenNewGroup={() => setShowNewGroup(true)}
                onOpenInvite={() => setShowInvite(true)}
                onOpenGroupSettings={() => setShowGroupSettings(true)}
                onOpenGroupEvents={() => setShowGroupEvents(true)}
                onLeaveGroup={handleLeaveGroup}
                user={user}
                groupDetail={groupDetail}
                filters={filters}
                isFilterApplied={isFilterApplied}
                onDraftChange={setFilters}
                onApplyFilters={handleApplyFilters}
                onEditFilters={handleEditFilters}
                onFiltersClear={handleClearFilters}
                presenceUserIds={presenceUserIds}
            />

            {isStale && (
                <div className="dashboard-stale-banner">
                    <span>Data may be outdated.</span>
                    <button className="stale-refresh-btn" onClick={manualRefresh}>
                        <FiRefreshCw size={14} className="stale-refresh-icon" /> Refresh
                    </button>
                </div>
            )}

            {groupDetail?.an !== undefined && groupDetail?.an !== null && (
                <div className={`dashboard-announcement${groupDetail.an ? " has-text" : " empty"}`}>
                    <strong>Announcement:</strong>{" "}
                    {groupDetail.an || <em>No announcement</em>}
                </div>
            )}

            {loadingDetail ? (
                <Spinner />
            ) : (
                <div className="dashboard-tasks">
                    <div className="dashboard-search">
                        <input
                            type="text"
                            className="dashboard-search-input"
                            placeholder="Search tasks by title…"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                        />
                        {searchQuery && (
                            <button
                                className="dashboard-search-clear"
                                onClick={() => setSearchQuery("")}
                            >
                                ✕
                            </button>
                        )}
                    </div>

                    <div className="dashboard-limits-bar">
                        <span className="dashboard-limit-tag" title="Groups you belong to">
                            Groups: {groups.length}/3
                        </span>
                        <span className="dashboard-limit-tag" title="Tasks in this group">
                            Tasks: {groupDetail?.tp?.length ?? 0}/50
                        </span>
                    </div>

                    { }
                    <DashboardColumnHeaders
                        visCols={visCols}
                        gridTemplate={gridCols(colWidths, visCols) + (showDeleteColumn ? " 42px" : "")}
                        showDeleteColumn={showDeleteColumn}
                        onTitleHandleDown={onTitleHandleDown}
                        onPointerDown={onPointerDown}
                    />

                    {TASK_STATES.map((state) => (
                        <DashboardTaskSection
                            key={state}
                            state={state}
                            label={STATE_LABELS[state]}
                            collapsed={collapsedSections[state]}
                            tasks={tasksByState[state]}
                            onToggle={() => toggleSection(state)}
                            canManageTasks={canManageTasks}
                            showDeleteColumn={showDeleteColumn}
                            onOpenNewTask={handleOpenNewTask}
                            groupId={activeGroup?.id}
                            colWidths={colWidths}
                            visCols={visCols}
                            onDeleted={refreshActiveGroup}
                        />
                    ))}
                </div>
            )}

            {showNewGroup && (
                <NewGroupPopup
                    onClose={() => setShowNewGroup(false)}
                    onCreated={handleGroupCreated}
                />
            )}
            {showInvite && activeGroup && (
                <InviteToGroupPopup
                    groupId={activeGroup.id}
                    onClose={() => setShowInvite(false)}
                />
            )}
            {showGroupSettings && activeGroup && (
                <GroupSettingsPopup
                    group={activeGroup}
                    members={members}
                    user={user}
                    onClose={() => setShowGroupSettings(false)}
                    onUpdated={handleGroupUpdated}
                    onDeleted={() => {
                        setShowGroupSettings(false);
                        handleLeaveGroup("deleted");
                    }}
                />
            )}
            {showGroupEvents && activeGroup && (
                <GroupEventsPopup
                    groupId={activeGroup.id}
                    lastSeenGroupEvents={
                        members?.find((m) => m.user?.id === user?.id)?.lastSeenGroupEvents
                    }
                    onClose={() => {
                        setShowGroupEvents(false);
                        markGroupEventsSeen();
                    }}
                />
            )}
            {showNewTask && activeGroup && (
                <NewTaskPopup
                    groupId={activeGroup.id}
                    initialState={newTaskState}
                    members={members}
                    onClose={() => setShowNewTask(false)}
                    onCreated={handleTaskCreated}
                    onRefresh={refreshActiveGroup}
                />
            )}
        </div>
    );
}
