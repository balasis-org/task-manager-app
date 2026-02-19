import { useState, useEffect, useContext, useCallback, useRef } from "react";
import { GroupContext } from "@context/GroupContext";
import { AuthContext } from "@context/AuthContext";
import { useNavigate } from "react-router-dom";
import NewGroupPopup from "@components/popups/NewGroupPopup";
import InviteToGroupPopup from "@components/popups/InviteToGroupPopup";
import GroupSettingsPopup from "@components/popups/GroupSettingsPopup";
import GroupEventsPopup from "@components/popups/GroupEventsPopup";
import NewTaskPopup from "@components/popups/NewTaskPopup";
import DashboardTopBar from "@components/dashboard/DashboardTopBar";
import TaskTable from "@components/dashboard/TaskTable";
import Spinner from "@components/Spinner";
import { useToast } from "@context/ToastContext";
import { apiGet } from "@assets/js/apiClient.js";
import { FILTER_EMPTY, isFilterEmpty } from "@components/dashboard/FilterPanel";
import { FiPlus, FiRefreshCw } from "react-icons/fi";
import "@styles/pages/Dashboard.css";

const TASK_STATES = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];
const STATE_LABELS = {TODO: "TODO",IN_PROGRESS: "In progress",TO_BE_REVIEWED: "To be reviewed",DONE: "Done"};

const COL_NAMES   = ["Title", "Creator", "Priority", "Due date", "Accessible", "\uD83D\uDCAC"];
const COL_CLASSES = ["col-title", "col-creator", "col-priority", "col-due", "col-access", "col-comments"];
const COL_DEFAULTS = [1, 130, 90, 120, 90, 60];  // first col is flex
const COL_MIN      = [120, 70, 60, 80, 60, 40];

// column indices are visible at each breakpoint
function visibleCols(width) {
    if (width <= 480) return [0, 2, 5];                // title + priority + comments
    if (width <= 768) return [0, 2, 3, 4, 5];          // hide creator + due => actually hide creator(1) and due(3)
    if (width <= 768) return [0, 2, 4, 5];
    return [0, 1, 2, 3, 4, 5];                         // all
}

// builds the grid-template-columns css value for visible columns only
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
        reloadGroups,
        manualRefresh,
        markGroupEventsSeen,
    } = useContext(GroupContext);
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const showToast = useToast();

    // refresh when coming back to dashboard
    useEffect(() => {
        if (activeGroup) refreshActiveGroup();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // listen for group-access-lost (fired by GroupProvider when a 403/404 on refresh)
    useEffect(() => {
        const handler = (e) => {
            const msg = e.detail?.message || "You no longer have access to this group.";
            showToast(msg, "error");
        };
        window.addEventListener("group-access-lost", handler);
        return () => window.removeEventListener("group-access-lost", handler);
    }, [showToast]);

    // track viewport width for responsive column visibility
    const [vpWidth, setVpWidth] = useState(() => window.innerWidth);
    useEffect(() => {
        const onResize = () => setVpWidth(window.innerWidth);
        window.addEventListener("resize", onResize);
        return () => window.removeEventListener("resize", onResize);
    }, []);
    const visCols = visibleCols(vpWidth);

    // resizable columns — dragging a handle grows one side and shrinks the other
    const [colWidths, setColWidths] = useState(COL_DEFAULTS);

    const onPointerDown = useCallback((leftIdx, e) => {
        e.preventDefault();
        const rightIdx = leftIdx + 1;
        if (rightIdx >= COL_DEFAULTS.length) return; // last col has no right neighbor
        const startX = e.clientX;
        const startL = colWidths[leftIdx];
        const startR = colWidths[rightIdx];

        function onMove(ev) {
            const dx = ev.clientX - startX;
            setColWidths((prev) => {
                const next = [...prev];
                const newL = Math.max(COL_MIN[leftIdx], startL + dx);
                const newR = Math.max(COL_MIN[rightIdx], startR - dx);
                // only apply if both sides stay >= min
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

    // Resize handle for the title column's right edge.
    // Since title is 1fr (flexible), dragging adjusts the adjacent fixed column.
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

    // ── Filter state ──
    const [filters, setFilters] = useState({ ...FILTER_EMPTY });
    const [filterIds, setFilterIds] = useState(null);   // Set<number> or null
    const [isFilterApplied, setIsFilterApplied] = useState(false);
    const filterVersionRef = useRef(0);

    // Build query string from filter criteria
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

    // Explicit apply — called by FilterPanel's Apply button
    const handleApplyFilters = useCallback(() => {
        if (!activeGroup?.id || isFilterEmpty(filters)) return;
        setIsFilterApplied(true);
        fetchFilterIds(activeGroup.id, filters);
    }, [activeGroup?.id, filters]);

    // Unlock fields for editing (keep current filterIds visible)
    const handleEditFilters = useCallback(() => {
        setIsFilterApplied(false);
    }, []);

    // Clear everything
    const handleClearFilters = useCallback(() => {
        setFilters({ ...FILTER_EMPTY });
        setFilterIds(null);
        setIsFilterApplied(false);
    }, []);

    // Re-fetch filter IDs when groupDetail changes (polling) and filter is applied
    const filtersRef = useRef(filters);
    filtersRef.current = filters;
    const isFilterAppliedRef = useRef(isFilterApplied);
    isFilterAppliedRef.current = isFilterApplied;
    useEffect(() => {
        if (isFilterAppliedRef.current && !isFilterEmpty(filtersRef.current) && activeGroup?.id && groupDetail) {
            fetchFilterIds(activeGroup.id, filtersRef.current);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [groupDetail]);

    // Reset filters when switching groups
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
        setShowGroupSettings(false);
        updateGroup(updatedGroup);
    }

    // "left" or "deleted" => remove group, "refresh" => just reload
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

    // group tasks by state + filter IDs whitelist + search filter
    const tasksByState = {};
    for (const s of TASK_STATES) tasksByState[s] = [];
    if (groupDetail?.tp) {                                          // tp = taskPreviews
        const q = searchQuery.toLowerCase().trim();
        for (const task of groupDetail.tp) {
            // 1. Filter by backend IDs whitelist (when filter is active)
            if (filterIds && !filterIds.has(task.i)) continue;      // i = id
            // 2. Then local search
            if (q && !task.t?.toLowerCase().includes(q)) continue;  // t = title
            const state = task.ts || "TODO";                        // ts = taskState
            if (tasksByState[state]) {
                tasksByState[state].push(task);
            }
        }
    }

    if (!loadingGroups && groups.length === 0) {
        return (
            <div className="dashboard-empty">
                <h2>Welcome!</h2>
                <p>You don't have any groups yet. Create one to get started.</p>
                <button
                    className="btn-primary"
                    onClick={() => setShowNewGroup(true)}
                >
                    + Create a group
                </button>
                {showNewGroup && (
                    <NewGroupPopup
                        onClose={() => setShowNewGroup(false)}
                        onCreated={handleGroupCreated}
                    />
                )}
            </div>
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

                    {/* column headers + resize handles */}
                    <div
                        className="task-col-header"
                        style={{ gridTemplateColumns: gridCols(colWidths, visCols) }}
                    >
                        {visCols.map((ci) => (
                            <span key={ci} className={`col-header-cell ${COL_CLASSES[ci]}`}>
                                {COL_NAMES[ci]}
                                {/* Title column: resize handle on the right, adjusts the next fixed column */}
                                {ci === 0 && visCols.length > 1 && (
                                    <span
                                        className="col-resize-handle"
                                        onPointerDown={(e) => onTitleHandleDown(visCols[1], e)}
                                    />
                                )}
                                {/* Other columns: resize handle between adjacent visible columns (skip last) */}
                                {ci !== 0 && ci !== visCols[visCols.length - 1] && (
                                    <span
                                        className="col-resize-handle"
                                        onPointerDown={(e) => onPointerDown(ci, e)}
                                    />
                                )}
                            </span>
                        ))}
                    </div>

                    {TASK_STATES.map((state) => (
                        <div key={state} className="task-section">
                            <div
                                className="task-section-header"
                                onClick={() => toggleSection(state)}
                            >
                                <span className="task-section-arrow">
                                    {collapsedSections[state] ? "▶" : "▼"}
                                </span>
                                <span className="task-section-title">
                                    {STATE_LABELS[state]}
                                    {canManageTasks && (
                                        <button
                                            className="task-section-add"
                                            title="Create task"
                                            onClick={(e) => handleOpenNewTask(e, state)}
                                        >
                                            <FiPlus size={12} />
                                        </button>
                                    )}
                                </span>
                                <span className="task-section-count">
                                    ({tasksByState[state].length})
                                </span>
                            </div>
                            {!collapsedSections[state] && (
                                <TaskTable
                                    tasks={tasksByState[state]}
                                    groupId={activeGroup?.id}
                                    colWidths={colWidths}
                                    visCols={visCols}
                                />
                            )}
                        </div>
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
