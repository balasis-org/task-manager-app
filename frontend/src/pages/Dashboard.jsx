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
import GroupFiles from "@components/dashboard/GroupFiles";
import Spinner from "@components/Spinner";
import { useToast } from "@context/ToastContext";
import { groupFileLimits } from "@assets/js/inputValidation";
import { apiGet } from "@assets/js/apiClient.js";
import { formatFileSize } from "@assets/js/fileUtils";
import { FILTER_EMPTY, isFilterEmpty } from "@components/dashboard/FilterPanel";
import { FiRefreshCw } from "react-icons/fi";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Dashboard.css";

// main dashboard: group selector in top bar, task cards grouped by state column,
// group files gallery tab, server-side filtering, and stale-banner when GroupContext
// polling stops. delegates rendering to DashboardTopBar, DashboardTaskSection, etc.
const TASK_STATES = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];
const STATE_LABELS = {TODO: "To do",IN_PROGRESS: "In progress",TO_BE_REVIEWED: "To be reviewed",DONE: "Done"};

/* fixed column widths - rem so they stay identical regardless of local font-size */
const COL_WIDTHS    = ["minmax(7rem,1fr)", "6.5rem", "7.5rem", "6.25rem", "3rem", "3rem"];
const COL_WIDTHS_SM = ["minmax(7rem,1fr)", "6.5rem", "3.75rem", "6.25rem", "3rem", "3rem"];

function pxToEm(px) {
    return px / parseFloat(getComputedStyle(document.documentElement).fontSize);
}

function visibleCols(widthPx) {
    const w = pxToEm(widthPx);
    if (w <= 48) return [0, 2, 3, 4];       // hide Creator + Comments
    if (w <= 64) return [0, 2, 3, 4, 5];    // hide Creator
    return [0, 1, 2, 3, 4, 5];
}

function gridCols(vis, widthPx) {
    const widths = pxToEm(widthPx) <= 48 ? COL_WIDTHS_SM : COL_WIDTHS;
    return vis.map((ci) => widths[ci]).join(" ");
}

function deleteColWidth(widthPx) {
    return pxToEm(widthPx) <= 48 ? "1.75rem" : "2.625rem";
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
    const fileLimits = groupFileLimits(groupDetail);

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
    const gridTemplate = gridCols(visCols, vpWidth);
    const delColW = deleteColWidth(vpWidth);

    const [collapsedSections, setCollapsedSections] = useState({});
    const [topBarOpen, setTopBarOpen] = useState(true);
    const [searchQuery, setSearchQuery] = useState("");
    const [hideInaccessible, setHideInaccessible] = useState(false);
    const [activeTab, setActiveTab] = useState("tasks");

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

            if (hideInaccessible && task.a === false) continue;

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
                user={user}
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
                searchQuery={searchQuery}
                onSearchChange={setSearchQuery}
            />

            {isStale && (
                <div className="dashboard-stale-banner">
                    <span>Data may be outdated.</span>
                    <button className="stale-refresh-btn" onClick={manualRefresh}>
                        <FiRefreshCw size={14} className="stale-refresh-icon" /> Refresh
                    </button>
                </div>
            )}

            {user?.downgradeGraceDeadline && new Date(user.downgradeGraceDeadline) > new Date() && (
                <div className="dashboard-downgrade-banner">
                    <strong>Plan downgraded.</strong>{" "}
                    You have until{" "}
                    {new Date(user.downgradeGraceDeadline).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}{" "}
                    to bring your storage within budget. After that, excess files will be automatically removed
                    (unshielded groups first, completed tasks first, oldest first).
                </div>
            )}

            {groupDetail?.an !== undefined && groupDetail?.an !== null && (
                <div className={`dashboard-announcement${groupDetail.an ? " has-text" : " empty"}`}>
                    <strong>Announcement:</strong>{" "}
                    {groupDetail.an || <em>No announcement</em>}
                </div>
            )}

            {groupDetail?.d && (
                <div className="dashboard-description">
                    <strong>About:</strong> {groupDetail.d}
                </div>
            )}

            {loadingDetail ? (
                <Spinner />
            ) : (
                <div className="dashboard-tasks">
                    <div className="dashboard-limits-bar">
                        {groupDetail?.op && (
                            <span className="dashboard-limit-tag dashboard-tier-badge" title="Group owner's plan">
                                {groupDetail.op}
                            </span>
                        )}
                        <span className="dashboard-limit-tag" title="Groups you belong to">
                            Groups: {groups.length}/{user?.maxGroups ?? "?"}
                        </span>
                        <span className="dashboard-limit-tag" title="Tasks in this group">
                            Tasks: {groupDetail?.tp?.length ?? 0}/{fileLimits.maxTasks}
                        </span>
                        {groupDetail?.db > 0 && (
                            <span
                                className="dashboard-limit-tag"
                                title={`Owner's download budget this month${groupDetail?.ddce ? ". Repeat download guard is ON — each member can only download the same file once per day" : ""}`}
                            >
                                Downloads: {formatFileSize(groupDetail.udb ?? 0)}/{formatFileSize(groupDetail.db)}
                                {groupDetail?.ddce && <span className="dashboard-guard-badge">🛡</span>}
                            </span>
                        )}
                        {groupDetail?.sb > 0 && (
                            <span className="dashboard-limit-tag" title="Owner's storage usage">
                                Storage: {formatFileSize(groupDetail.usb ?? 0)}/{formatFileSize(groupDetail.sb)}
                            </span>
                        )}
                        <button
                            className={`dashboard-access-toggle${hideInaccessible ? " active" : ""}`}
                            onClick={() => setHideInaccessible((v) => !v)}
                            title={hideInaccessible ? "Showing accessible only - click to show all" : "Hide tasks not accessible to you"}
                        >
                            {hideInaccessible ? "\uD83D\uDD13 Accessible only" : "\uD83D\uDD12 Show all"}
                        </button>

                        <div className="dashboard-tab-toggle">
                            <button
                                className={`dashboard-tab-btn${activeTab === "tasks" ? " active" : ""}`}
                                onClick={() => setActiveTab("tasks")}
                            >
                                Tasks
                            </button>
                            <button
                                className={`dashboard-tab-btn${activeTab === "files" ? " active" : ""}`}
                                onClick={() => setActiveTab("files")}
                            >
                                Files
                            </button>
                        </div>
                    </div>

                    { }
                    {activeTab === "tasks" ? (
                        <>
                            <DashboardColumnHeaders
                                visCols={visCols}
                                gridTemplate={gridTemplate + (showDeleteColumn ? " " + delColW : "")}
                                showDeleteColumn={showDeleteColumn}
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
                                    gridTemplate={gridTemplate}
                                    visCols={visCols}
                                    onDeleted={refreshActiveGroup}
                                    deleteColWidth={delColW}
                                />
                            ))}
                        </>
                    ) : (
                        <GroupFiles groupId={activeGroup?.id} />
                    )}
                </div>
            )}

            {showNewGroup && (
                <NewGroupPopup
                    onClose={() => setShowNewGroup(false)}
                    onCreated={handleGroupCreated}
                    user={user}
                />
            )}
            {showInvite && activeGroup && (
                <InviteToGroupPopup
                    groupId={activeGroup.id}
                    groupDetail={groupDetail}
                    onClose={() => setShowInvite(false)}
                />
            )}
            {showGroupSettings && activeGroup && (
                <GroupSettingsPopup
                    group={activeGroup}
                    groupDetail={groupDetail}
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
                    isLeader={myRole === "GROUP_LEADER"}
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
                    groupDetail={groupDetail}
                    onClose={() => setShowNewTask(false)}
                    onCreated={handleTaskCreated}
                    onRefresh={refreshActiveGroup}
                    maxCreatorFiles={fileLimits.maxCreatorFiles}
                    maxAssigneeFiles={fileLimits.maxAssigneeFiles}
                    maxFileSizeBytes={fileLimits.maxFileSizeBytes}
                />
            )}
        </div>
    );
}
