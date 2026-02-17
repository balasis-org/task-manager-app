import { useState, useContext } from "react";
import { GroupContext } from "@context/GroupContext";
import { useNavigate } from "react-router-dom";
import NewGroupPopup from "@components/popups/NewGroupPopup";
import InviteToGroupPopup from "@components/popups/InviteToGroupPopup";
import GroupSettingsPopup from "@components/popups/GroupSettingsPopup";
import GroupEventsPopup from "@components/popups/GroupEventsPopup";
import DashboardTopBar from "@components/dashboard/DashboardTopBar";
import TaskTable from "@components/dashboard/TaskTable";
import Spinner from "@components/Spinner";
import { FiPlus } from "react-icons/fi";
import "@styles/pages/Dashboard.css";

const TASK_STATES = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];
const STATE_LABELS = {TODO: "TODO",IN_PROGRESS: "In progress",TO_BE_REVIEWED: "To be reviewed",DONE: "Done"};

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
    } = useContext(GroupContext);
    const navigate = useNavigate();

    // Collapsed sections
    const [collapsedSections, setCollapsedSections] = useState({});
    const [topBarOpen, setTopBarOpen] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");

    // Popups
    const [showNewGroup, setShowNewGroup] = useState(false);
    const [showInvite, setShowInvite] = useState(false);
    const [showGroupSettings, setShowGroupSettings] = useState(false);
    const [showGroupEvents, setShowGroupEvents] = useState(false);

    function handleGroupCreated(newGroup) {
        setShowNewGroup(false);
        addGroup(newGroup);
    }

    function handleGroupUpdated(updatedGroup) {
        setShowGroupSettings(false);
        updateGroup(updatedGroup);
    }

    function toggleSection(state) {
        setCollapsedSections((prev) => ({
            ...prev,
            [state]: !prev[state],
        }));
    }

    // Role-based visibility
    const canManageTasks = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";

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

                    {/* Single column header — rendered once */}
                    <div className="task-col-header">
                        <span>Title</span>
                        <span>Creator</span>
                        <span>Priority</span>
                        <span>Due date</span>
                        <span>Accessible</span>
                        <span style={{ textAlign: "center" }}>💬</span>
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
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                navigate(
                                                    `/task?groupId=${activeGroup?.id}&new=1&state=${state}`
                                                );
                                            }}
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
                    onClose={() => setShowGroupSettings(false)}
                    onUpdated={handleGroupUpdated}
                />
            )}
            {showGroupEvents && activeGroup && (
                <GroupEventsPopup
                    groupId={activeGroup.id}
                    onClose={() => setShowGroupEvents(false)}
                />
            )}
        </div>
    );
}
