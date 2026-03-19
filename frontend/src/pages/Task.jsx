// single-task view: inline-editable title/description, state transition bar,
// file upload sections (creator + assignee), participant management, review panel,
// email notifications. uses useSmartPoll on /has-task-changed for live updates.
import { useState, useEffect, useContext, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FiChevronRight, FiChevronLeft, FiRefreshCw, FiMail } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { GroupContext } from "@context/GroupContext";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPatch, apiPost, apiDelete } from "@assets/js/apiClient.js";
import { LIMITS, groupFileLimits } from "@assets/js/inputValidation";
import { isFileTooLarge, formatFileSize } from "@assets/js/fileUtils";
import useSmartPoll from "@hooks/useSmartPoll";
import { useBlobUrl } from "@context/BlobSasContext";
import Spinner from "@components/Spinner";
import TaskBreadcrumb from "@components/task/TaskBreadcrumb";
import TaskStateBar from "@components/task/TaskStateBar";
import TaskBody from "@components/task/TaskBody";
import TaskFilesSection from "@components/task/TaskFilesSection";
import TaskReviewPanel from "@components/task/TaskReviewPanel";
import TaskAssigneePanel from "@components/task/TaskAssigneePanel";
import TaskDeleteModal from "@components/task/TaskDeleteModal";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Task.css";

const REVIEWER_ELIGIBLE_ROLES = ["REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];
const ASSIGNEE_ELIGIBLE_ROLES = ["MEMBER", "REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];

export default function Task() {
    const { groupId, taskId } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);
    const { activeGroup, myRole, members, presenceUserIds, refreshActiveGroup, groupDetail } = useContext(GroupContext);
    const blobUrl = useBlobUrl();
    const showToast = useToast();
    const fileLimits = groupFileLimits(groupDetail);

    usePageTitle("Task");

    const [task, setTask] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editing, setEditing] = useState(false);
    const [rightOpen, setRightOpen] = useState(true);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

    const [editTitle, setEditTitle] = useState("");
    const [editDesc, setEditDesc] = useState("");
    const [editState, setEditState] = useState("");

    const [reviewComment, setReviewComment] = useState("");
    const [reviewDecision, setReviewDecision] = useState("APPROVE");
    const [submittingReview, setSubmittingReview] = useState(false);

    const [showReviewerPicker, setShowReviewerPicker] = useState(false);
    const [showAssigneePicker, setShowAssigneePicker] = useState(false);
    const [downloadingId, setDownloadingId] = useState(null);

    // Email notification dialog state
    const [notifyTarget, setNotifyTarget] = useState(null); // { userId, name }
    const [notifyNote, setNotifyNote] = useState("");
    const [notifySending, setNotifySending] = useState(false);

    const lastFetchRef = useRef(new Date().toISOString());

    /* -- polling -- */
    const taskPollCheck = useCallback(async () => {
        if (!groupId || !taskId) return;
        await apiGet(`/api/groups/${groupId}/task/${taskId}/has-changed?since=${encodeURIComponent(lastFetchRef.current)}`);
    }, [groupId, taskId]);

    const {
        hasChanged: taskHasChanged,
        isStale: taskIsStale,
        reset: resetTaskPoll,
    } = useSmartPoll(taskPollCheck, { enabled: !!groupId && !!taskId });

    /* -- derived / permissions -- */
    const isLeaderOrManager = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";
    const myParticipant = task?.taskParticipants
        ? [...task.taskParticipants].find((p) => p.user?.id === user?.id)
        : null;
    const myTaskRole = myParticipant?.taskParticipantRole || null;
    const isReviewer = myTaskRole === "REVIEWER";
    const isAssignee = myTaskRole === "ASSIGNEE";
    const canEdit = isLeaderOrManager;

    const canDelete = (() => {
        if (!isLeaderOrManager || !task) return false;
        if (myRole === "GROUP_LEADER") return true;
        const creatorParticipant = task.taskParticipants?.find(p => p.taskParticipantRole === "CREATOR");
        if (!creatorParticipant) return true;
        if (creatorParticipant.user?.id === user?.id) return true;
        const creatorMember = (members || []).find(m => m.user?.id === creatorParticipant.user?.id);
        if (!creatorMember) return true;
        return creatorMember.role !== "GROUP_LEADER" && creatorMember.role !== "TASK_MANAGER";
    })();
    const canReview = isReviewer || isLeaderOrManager;
    const canReviewFiles = canReview && task?.taskState === "TO_BE_REVIEWED";
    const canMoveToBeReviewed = isAssignee;
    const canChangeState = isLeaderOrManager;
    const canAddParticipants = isLeaderOrManager;
    const canUploadFiles = isLeaderOrManager;
    const canUploadAssigneeFiles = isAssignee || isLeaderOrManager;

    // Prefer per-task effective limits from the backend; fall back to group-level
    const effectiveMaxCreatorFiles  = task?.effectiveMaxCreatorFiles  ?? fileLimits.maxCreatorFiles;
    const effectiveMaxAssigneeFiles = task?.effectiveMaxAssigneeFiles ?? fileLimits.maxAssigneeFiles;
    const effectiveMaxFileSizeBytes = task?.effectiveMaxFileSizeBytes ?? fileLimits.maxFileSizeBytes;

    const reviewers = task?.taskParticipants
        ? [...task.taskParticipants].filter((p) => p.taskParticipantRole === "REVIEWER") : [];
    const assignees = task?.taskParticipants
        ? [...task.taskParticipants].filter((p) => p.taskParticipantRole === "ASSIGNEE") : [];
    const creator = task?.taskParticipants
        ? [...task.taskParticipants].find((p) => p.taskParticipantRole === "CREATOR") : null;

    const existingReviewerIds = new Set(reviewers.map((r) => r.user?.id));
    const existingAssigneeIds = new Set(assignees.map((a) => a.user?.id));
    const eligibleReviewers = (members || []).filter(
        (m) => REVIEWER_ELIGIBLE_ROLES.includes(m.role) && !existingReviewerIds.has(m.user?.id)
    );
    const eligibleAssignees = (members || []).filter(
        (m) => ASSIGNEE_ELIGIBLE_ROLES.includes(m.role) && !existingAssigneeIds.has(m.user?.id)
    );

    /* -- effects -- */
    useEffect(() => { if (groupId) refreshActiveGroup(); }, [groupId]);

    useEffect(() => {
        const handler = (e) => {
            if (String(e.detail?.groupId) === String(groupId)) {
                showToast(e.detail?.message || "You no longer have access to this group.", "error");
                navigate("/dashboard", { replace: true });
            }
        };
        window.addEventListener("group-access-lost", handler);
        return () => window.removeEventListener("group-access-lost", handler);
    }, [groupId, navigate, showToast]);

    function applyTask(data) {
        setTask(data);
        setEditTitle(data.title || "");
        setEditDesc(data.description || "");
        setEditState(data.taskState || "TODO");
        lastFetchRef.current = new Date().toISOString();
        resetTaskPoll();
    }

    function refetchTask() {
        if (!groupId || !taskId) return;
        apiGet(`/api/groups/${groupId}/task/${taskId}`).then(applyTask).catch(() => {});
    }

    function autoHeal() { refreshActiveGroup(); refetchTask(); }

    useEffect(() => {
        if (!groupId || !taskId) return;
        setLoading(true);
        setError(null);
        apiGet(`/api/groups/${groupId}/task/${taskId}`)
            .then(applyTask)
            .catch(() => setError("Failed to load task"))
            .finally(() => setLoading(false));
    }, [groupId, taskId]);

    /* -- handlers -- */
    async function handleDeleteTask() {
        try {
            await apiDelete(`/api/groups/${groupId}/task/${taskId}`);
            showToast("Task deleted.", "success");
            navigate("/dashboard");
        } catch (err) {
            showToast(err?.message || "Failed to delete task");
            setShowDeleteConfirm(false);
        }
    }

    async function handleSave() {
        try {
            const body = {};
            if (editTitle !== task.title) body.title = editTitle;
            if (editDesc !== task.description) body.description = editDesc;
            if (editState !== task.taskState) body.taskState = editState;
            const updated = await apiPatch(`/api/groups/${groupId}/task/${taskId}`, body);
            applyTask(updated);
            setEditing(false);
        } catch (err) { showToast(err?.message || "Failed to save changes"); autoHeal(); }
    }

    async function handleStateChange(newState) {
        try {
            const updated = await apiPatch(`/api/groups/${groupId}/task/${taskId}`, { taskState: newState });
            applyTask(updated);
        } catch (err) { showToast(err?.message || "Failed to change state"); autoHeal(); }
    }

    async function handleReview() {
        setSubmittingReview(true);
        try {
            const updated = await apiPost(`/api/groups/${groupId}/task/${taskId}/review`, {
                reviewComment, reviewersDecision: reviewDecision,
            });
            applyTask(updated);
            setReviewComment("");
        } catch (err) { showToast(err?.message || "Failed to submit review"); autoHeal(); }
        finally { setSubmittingReview(false); }
    }

    async function handleAddParticipant(userId, role) {
        try {
            const updated = await apiPost(
                `/api/groups/${groupId}/task/${taskId}/taskParticipants`,
                { userId, taskParticipantRole: role }
            );
            applyTask(updated);
            if (role === "REVIEWER") setShowReviewerPicker(false);
            else setShowAssigneePicker(false);
        } catch (err) { showToast(err?.message || "Failed to add participant"); autoHeal(); }
    }

    async function handleRemoveParticipant(participantId) {
        try {
            await apiDelete(`/api/groups/${groupId}/task/${taskId}/taskParticipant/${participantId}`);
            const refreshed = await apiGet(`/api/groups/${groupId}/task/${taskId}`);
            applyTask(refreshed);
        } catch (err) { showToast(err?.message || "Failed to remove participant"); autoHeal(); }
    }

    async function handleNotifyParticipant(userId) {
        // Find user name from assignees or reviewers for the dialog
        const all = [...(task?.assignees || []), ...(task?.reviewers || [])];
        const match = all.find(p => p.user?.id === userId);
        const name = match?.user?.name || match?.user?.email || "this user";
        setNotifyTarget({ userId, name });
        setNotifyNote("");
    }

    function handleBulkNotify(type) {
        let ids = [];
        let label = "";
        if (type === "assignees") {
            ids = assignees.map(a => a.user?.id).filter(Boolean);
            label = "all assignees";
        } else if (type === "reviewers") {
            ids = reviewers.map(r => r.user?.id).filter(Boolean);
            label = "all reviewers";
        } else {
            ids = [...new Set([
                ...assignees.map(a => a.user?.id),
                ...reviewers.map(r => r.user?.id),
            ].filter(Boolean))];
            label = "all participants";
        }
        if (ids.length === 0) { showToast("No participants to notify"); return; }
        setNotifyTarget({ userIds: ids, name: label });
        setNotifyNote("");
    }

    async function sendNotification() {
        if (!notifyTarget) return;
        setNotifySending(true);
        try {
            const body = notifyNote.trim() ? { customNote: notifyNote.trim() } : {};
            if (notifyTarget.userIds) {
                body.userIds = notifyTarget.userIds;
                await apiPost(`/api/groups/${groupId}/task/${taskId}/notify-bulk`, body);
            } else {
                await apiPost(`/api/groups/${groupId}/task/${taskId}/notify/${notifyTarget.userId}`,
                    notifyNote.trim() ? { customNote: notifyNote.trim() } : undefined);
            }
            showToast("Notification sent", "success");
            setNotifyTarget(null);
        } catch (err) { showToast(err?.message || "Failed to send notification"); }
        finally { setNotifySending(false); }
    }

    async function handleMoveToBeReviewed() {
        try {
            const updated = await apiPost(`/api/groups/${groupId}/task/${taskId}/to-be-reviewed`);
            applyTask(updated);
        } catch (err) { showToast(err?.message || "Failed to move task to review"); autoHeal(); }
    }

    async function handleFileAdd(file, isAssignee) {
        const currentCount = isAssignee ? (task?.assigneeFiles?.length || 0) : (task?.files?.length || 0);
        const maxFiles = isAssignee ? effectiveMaxAssigneeFiles : effectiveMaxCreatorFiles;
        if (currentCount >= maxFiles) { showToast(`Maximum ${maxFiles} files already uploaded`); return; }
        if (isFileTooLarge(file, effectiveMaxFileSizeBytes)) {
            showToast(`File exceeds ${formatFileSize(effectiveMaxFileSizeBytes)} limit`);
            return;
        }
        const fd = new FormData();
        fd.append("file", file);
        const endpoint = isAssignee
            ? `/api/groups/${groupId}/task/${taskId}/assignee-files`
            : `/api/groups/${groupId}/task/${taskId}/files`;
        try { const updated = await apiPost(endpoint, fd); applyTask(updated); }
        catch (err) { showToast(err?.message || `Failed to upload ${isAssignee ? "assignee " : ""}file`); autoHeal(); }
    }

    async function handleDownload(fileId, filename, isAssignee) {
        setDownloadingId(fileId);
        const endpoint = isAssignee
            ? `/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}/download`
            : `/api/groups/${groupId}/task/${taskId}/files/${fileId}/download`;

        // Retry a few times on 503 (server momentarily busy) so the user
        // just sees the spinner a bit longer instead of an error toast.
        const MAX_RETRIES = 3;
        for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                const blob = await apiGet(endpoint, { responseType: "blob" });
                const url = URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = url; a.download = filename; a.click();
                URL.revokeObjectURL(url);
                setDownloadingId(null);
                return;
            } catch (err) {
                // 429 = download budget exhausted or repeat guard — show msg, no retry
                if (err?.status === 429) {
                    showToast(err.message || "Download limit reached");
                    break;
                }
                const is503 = err?.status === 503;
                if (is503 && attempt < MAX_RETRIES) {
                    await new Promise(r => setTimeout(r, 2000 * (attempt + 1)));
                    continue;
                }
                showToast(err?.message || "Failed to download file");
                break;
            }
        }
        setDownloadingId(null);
    }

    async function handleDeleteFile(fileId, isAssignee) {
        try {
            const endpoint = isAssignee
                ? `/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}`
                : `/api/groups/${groupId}/task/${taskId}/files/${fileId}`;
            await apiDelete(endpoint);
            const refreshed = await apiGet(`/api/groups/${groupId}/task/${taskId}`);
            applyTask(refreshed);
        } catch (err) { showToast(err?.message || "Failed to delete file"); autoHeal(); }
    }

    async function handleFileReview(fileId, isAssignee, status, note) {
        const endpoint = isAssignee
            ? `/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}/review`
            : `/api/groups/${groupId}/task/${taskId}/files/${fileId}/review`;
        try {
            await apiPost(endpoint, { status, note });
            const refreshed = await apiGet(`/api/groups/${groupId}/task/${taskId}`);
            applyTask(refreshed);
            showToast("File review saved", "success");
        } catch (err) { showToast(err?.message || "Failed to submit file review"); autoHeal(); }
    }

    /* -- guards -- */
    if (loading) return <Spinner />;
    if (error) return <div className="task-page-error">{error}</div>;
    if (!task) return <div className="task-page-error">Task not found</div>;

    const groupName = activeGroup?.name || `Group ${groupId}`;
    const canNotify = canAddParticipants && groupDetail?.op !== "FREE" && groupDetail?.op !== "STUDENT";

    /* -- render -- */
    return (
        <div className="task-page">
            <div className={`task-main${rightOpen ? "" : " full-width"}`}>
                <TaskBreadcrumb
                    groupId={groupId} taskId={taskId} groupName={groupName}
                    creator={creator} lastEditBy={task.lastEditBy}
                    canEdit={canEdit} canDelete={canDelete} editing={editing}
                    onEdit={() => setEditing(true)}
                    onDelete={() => setShowDeleteConfirm(true)}
                    presenceUserIds={presenceUserIds}
                />

                {(taskHasChanged || taskIsStale) && (
                    <div className="task-stale-banner">
                        <span>{taskHasChanged ? "This task has been updated." : "Data may be outdated."}</span>
                        <button className="stale-refresh-btn" onClick={refetchTask}>
                            <FiRefreshCw size={14} className="stale-refresh-icon" /> Refresh
                        </button>
                    </div>
                )}

                <TaskStateBar
                    canChange={canChangeState || canEdit}
                    editing={editing} editState={editState} taskState={task.taskState}
                    onEditStateChange={setEditState} onStateChange={handleStateChange}
                />

                <TaskBody
                    editing={editing} task={task}
                    editTitle={editTitle} editDesc={editDesc}
                    onTitleChange={setEditTitle} onDescChange={setEditDesc}
                    onSave={handleSave} onCancel={() => setEditing(false)}
                >
                    <TaskFilesSection
                        files={task.files || []} maxFiles={effectiveMaxCreatorFiles}
                        canUpload={canUploadFiles} canReview={canReviewFiles}
                        label="Files" emptyText="No files"
                        className="task-files-section"
                        onFileAdd={(f) => handleFileAdd(f, false)}
                        onDownload={(id, name) => handleDownload(id, name, false)}
                        onDelete={(id) => handleDeleteFile(id, false)}
                        onFileReview={(fileId, status, note) => handleFileReview(fileId, false, status, note)}
                        downloadingId={downloadingId}
                    />
                </TaskBody>
            </div>

            <aside className={`task-right-sidebar${rightOpen ? "" : " collapsed"}`}>
                <button
                    className="task-right-toggle"
                    onClick={() => setRightOpen((v) => !v)}
                    title={rightOpen ? "Hide panel" : "Show panel"}
                >
                    {rightOpen ? <FiChevronRight size={14} /> : <FiChevronLeft size={14} />}
                </button>

                    <div className="task-right-content">
                        <TaskReviewPanel
                            reviewers={reviewers} eligibleReviewers={eligibleReviewers}
                            showPicker={showReviewerPicker}
                            onTogglePicker={() => { setShowReviewerPicker((v) => !v); setShowAssigneePicker(false); }}
                            canAddParticipants={canAddParticipants}
                            onAddReviewer={(uid) => handleAddParticipant(uid, "REVIEWER")}
                            onRemoveParticipant={canAddParticipants ? handleRemoveParticipant : undefined}
                            onNotify={canNotify ? handleNotifyParticipant : undefined}
                            onBulkNotify={canNotify ? handleBulkNotify : undefined}
                            blobUrl={blobUrl} task={task}
                            canReview={canReview}
                            reviewComment={reviewComment} onReviewCommentChange={setReviewComment}
                            reviewDecision={reviewDecision} onReviewDecisionChange={setReviewDecision}
                            submittingReview={submittingReview} onReview={handleReview}
                            presenceUserIds={presenceUserIds}
                        />

                        <TaskAssigneePanel
                            assignees={assignees} eligibleAssignees={eligibleAssignees}
                            showPicker={showAssigneePicker}
                            onTogglePicker={() => { setShowAssigneePicker((v) => !v); setShowReviewerPicker(false); }}
                            canAddParticipants={canAddParticipants}
                            onAddAssignee={(uid) => handleAddParticipant(uid, "ASSIGNEE")}
                            onRemoveParticipant={canAddParticipants ? handleRemoveParticipant : undefined}
                            onNotify={canNotify ? handleNotifyParticipant : undefined}
                            onBulkNotify={canNotify ? handleBulkNotify : undefined}
                            blobUrl={blobUrl}
                            presenceUserIds={presenceUserIds}
                        />

                        {canNotify && (assignees.length + reviewers.length) > 0 && (
                            <div className="task-sidebar-section">
                                <button
                                    className="task-bulk-all-btn"
                                    onClick={() => handleBulkNotify("both")}
                                >
                                    <FiMail size={14} /> Email all participants
                                </button>
                            </div>
                        )}

                        <TaskFilesSection
                            files={task.assigneeFiles || []} maxFiles={effectiveMaxAssigneeFiles}
                            canUpload={canUploadAssigneeFiles} canReview={canReviewFiles}
                            label="Assignee Files"
                            emptyText="No assignee files" className="task-sidebar-section"
                            onFileAdd={(f) => handleFileAdd(f, true)}
                            onDownload={(id, name) => handleDownload(id, name, true)}
                            onDelete={(id) => handleDeleteFile(id, true)}
                            onFileReview={(fileId, status, note) => handleFileReview(fileId, true, status, note)}
                            downloadingId={downloadingId}
                        />

                        {canMoveToBeReviewed && task.taskState === "IN_PROGRESS" && (
                            <div className="task-sidebar-section task-move-section">
                                <button className="btn-primary btn-block" onClick={handleMoveToBeReviewed}>
                                    Move to be reviewed
                                </button>
                            </div>
                        )}
                    </div>
            </aside>

            {showDeleteConfirm && (
                <TaskDeleteModal
                    onConfirm={handleDeleteTask}
                    onCancel={() => setShowDeleteConfirm(false)}
                />
            )}

            {notifyTarget && (
                <div className="popup-overlay" onClick={() => !notifySending && setNotifyTarget(null)}>
                    <div className="popup-card task-notify-dialog" onClick={e => e.stopPropagation()}>
                        <h3>Send notification to {notifyTarget.name}?</h3>
                        <p className="task-notify-hint">
                            A default message about this task will be included automatically.
                        </p>
                        <label>
                            Add a personal note (optional)
                            <textarea
                                className="task-notify-textarea"
                                value={notifyNote}
                                onChange={e => setNotifyNote(e.target.value.slice(0, LIMITS.EMAIL_CUSTOM_NOTE))}
                                placeholder="e.g. Please check the latest files…"
                                rows={3}
                                maxLength={LIMITS.EMAIL_CUSTOM_NOTE}
                            />
                            <span className="task-notify-counter">
                                {notifyNote.length}/{LIMITS.EMAIL_CUSTOM_NOTE}
                            </span>
                        </label>
                        <div className="popup-actions">
                            <button className="btn-secondary" onClick={() => setNotifyTarget(null)} disabled={notifySending}>
                                Cancel
                            </button>
                            <button className="btn-primary" onClick={sendNotification} disabled={notifySending}>
                                {notifySending ? "Sending…" : "Send"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
