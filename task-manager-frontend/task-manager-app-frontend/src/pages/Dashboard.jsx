import { useState, useEffect, useContext, useCallback } from "react";
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
import { FiPlus } from "react-icons/fi";
import "@styles/pages/Dashboard.css";

const TASK_STATES = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];
const STATE_LABELS = {TODO: "TODO",IN_PROGRESS: "In progress",TO_BE_REVIEWED: "To be reviewed",DONE: "Done"};

const COL_NAMES   = ["Title", "Creator", "Priority", "Due date", "Accessible", "\uD83D\uDCAC"];
const COL_DEFAULTS = [1, 130, 90, 120, 90, 60];  // first col is flex
const COL_MIN      = [120, 70, 60, 80, 60, 40];

/* helper: build grid-template-columns string from widths array */
function gridCols(w) {
    return `1fr ${w.slice(1).map((v) => v + "px").join(" ")}`;
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
        selectGroup,
        addGroup,
        updateGroup,
        refreshActiveGroup,
        removeGroupFromState,
        reloadGroups,
    } = useContext(GroupContext);
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const showToast = useToast();

    // Refresh active group data when Dashboard mounts (e.g. navigating back from Task page)
    useEffect(() => {
        if (activeGroup) refreshActiveGroup();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ── Resizable columns ──
    // Dragging handle between col[i] and col[i+1] grows one and shrinks the other (Excel-style).
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

    // Collapsed sections
    const [collapsedSections, setCollapsedSections] = useState({});
    const [topBarOpen, setTopBarOpen] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");

    // Popups
    const [showNewGroup, setShowNewGroup] = useState(false);
    const [showInvite, setShowInvite] = useState(false);
    const [showGroupSettings, setShowGroupSettings] = useState(false);
    const [showGroupEvents, setShowGroupEvents] = useState(false);
    const [showNewTask, setShowNewTask] = useState(false);
    const [newTaskState, setNewTaskState] = useState("TODO");

    function handleGroupCreated(newGroup) {
        setShowNewGroup(false);
        addGroup(newGroup);
    }

    function handleGroupUpdated(updatedGroup) {
        setShowGroupSettings(false);
        updateGroup(updatedGroup);
    }

    /**
     * Called from DashboardTopBar when user leaves group or removes a member.
     * "left" = current user left → remove group from state
     * "refresh" = member removed → just refresh
     * "deleted" = group deleted → remove from state
     */
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

    // Role-based visibility
    const canManageTasks = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";

    // Task creation — open popup
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

    // Group tasks by state, filtered by search query
    const tasksByState = {};
    for (const s of TASK_STATES) tasksByState[s] = [];
    if (groupDetail?.taskPreviews) {
        const q = searchQuery.toLowerCase().trim();
        for (const task of groupDetail.taskPreviews) {
            if (q && !task.title?.toLowerCase().includes(q)) continue;
            const state = task.taskState || "TODO";
            if (tasksByState[state]) {
                tasksByState[state].push(task);
            }
        }
    }

    // ── No groups empty state ──
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
            {/* ── Top Bar ── */}
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
            />

            {/* Announcement */}
            {groupDetail?.announcement !== undefined && groupDetail?.announcement !== null && (
                <div className={`dashboard-announcement${groupDetail.announcement ? " has-text" : " empty"}`}>
                    <strong>Announcement:</strong>{" "}
                    {groupDetail.announcement || <em>No announcement</em>}
                </div>
            )}

            {/* ── Task Sections ── */}
            {loadingDetail ? (
                <Spinner />
            ) : (
                <div className="dashboard-tasks">
                    {/* Search input */}
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

                    {/* Single column header — rendered once, with drag handles */}
                    <div
                        className="task-col-header"
                        style={{ gridTemplateColumns: gridCols(colWidths) }}
                    >
                        {COL_NAMES.map((name, i) => (
                            <span key={i} className="col-header-cell">
                                {name}
                                {i < COL_NAMES.length - 1 && i >= 1 && (
                                    <span
                                        className="col-resize-handle"
                                        onPointerDown={(e) => onPointerDown(i, e)}
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
                                />
                            )}
                        </div>
                    ))}
                </div>
            )}

            {/* ── Popups ── */}
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
                        refreshActiveGroup();
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
