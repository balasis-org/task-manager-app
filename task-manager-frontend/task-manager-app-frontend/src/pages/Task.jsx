
import { useState, useEffect, useContext, useRef, useCallback } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { FiArrowLeft,FiMessageCircle,FiPlus,FiDownload,
    FiTrash2,FiEdit2,FiChevronRight,FiChevronLeft,FiRefreshCw} from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { GroupContext } from "@context/GroupContext";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPatch, apiPost, apiDelete } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import { getFileIcon, isFileTooLarge } from "@assets/js/fileUtils";
import useSmartPoll from "@hooks/useSmartPoll";
import blobBase from "@blobBase";
import Spinner from "@components/Spinner";
import "@styles/pages/Task.css";

const STATE_LABELS = {TODO: "TODO",IN_PROGRESS: "In Progress",
    TO_BE_REVIEWED: "To Be Reviewed",DONE: "Done"};
const STATE_OPTIONS = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];
const DECISION_OPTIONS = ["APPROVE", "REJECT"];
const REVIEWER_ELIGIBLE_ROLES = ["REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];
const ASSIGNEE_ELIGIBLE_ROLES = ["MEMBER", "REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];

export default function Task() {
    const { groupId, taskId } = useParams();
    const navigate = useNavigate();
    const { user } = useContext(AuthContext);
    const { activeGroup, myRole, members, refreshActiveGroup } = useContext(GroupContext);
    const showToast = useToast();

    const [task, setTask] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editing, setEditing] = useState(false);
    const [rightOpen, setRightOpen] = useState(true);

    // Editable fields
    const [editTitle, setEditTitle] = useState("");
    const [editDesc, setEditDesc] = useState("");
    const [editState, setEditState] = useState("");

    // Reviewer form
    const [reviewComment, setReviewComment] = useState("");
    const [reviewDecision, setReviewDecision] = useState("APPROVE");
    const [submittingReview, setSubmittingReview] = useState(false);

    // Participant picker popups
    const [showReviewerPicker, setShowReviewerPicker] = useState(false);
    const [showAssigneePicker, setShowAssigneePicker] = useState(false);

    // File uploads
    const fileInputRef = useRef(null);
    const assigneeFileInputRef = useRef(null);
    const [fileDragOver, setFileDragOver] = useState(false);
    const [assigneeDragOver, setAssigneeDragOver] = useState(false);
    const [downloadingId, setDownloadingId] = useState(null);

    // track when we last fetched the task for lightweight polling
    const lastFetchRef = useRef(new Date().toISOString());

    // lightweight change-detection poll
    const taskPollCheck = useCallback(async () => {
        if (!groupId || !taskId) return;
        await apiGet(`/api/groups/${groupId}/task/${taskId}/has-changed?since=${encodeURIComponent(lastFetchRef.current)}`);
    }, [groupId, taskId]);

    const {
        hasChanged: taskHasChanged,
        isStale: taskIsStale,
        reset: resetTaskPoll,
    } = useSmartPoll(taskPollCheck, { enabled: !!groupId && !!taskId });

    const isLeaderOrManager = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";
    const myParticipant = task?.taskParticipants
        ? [...task.taskParticipants].find((p) => p.user?.id === user?.id)
        : null;
    const myTaskRole = myParticipant?.taskParticipantRole || null;
    const isReviewer = myTaskRole === "REVIEWER";
    const isAssignee = myTaskRole === "ASSIGNEE";
    const canEdit = isLeaderOrManager;
    const canReview = isReviewer || isLeaderOrManager;
    const canMoveToBeReviewed = isAssignee;
    const canChangeState = isLeaderOrManager;
    const canAddParticipants = isLeaderOrManager;
    const canUploadFiles = isLeaderOrManager;
    const canUploadAssigneeFiles = isAssignee || isLeaderOrManager;

    // participant groupings
    const reviewers = task?.taskParticipants
        ? [...task.taskParticipants].filter((p) => p.taskParticipantRole === "REVIEWER")
        : [];
    const assignees = task?.taskParticipants
        ? [...task.taskParticipants].filter((p) => p.taskParticipantRole === "ASSIGNEE")
        : [];
    const creator = task?.taskParticipants
        ? [...task.taskParticipants].find((p) => p.taskParticipantRole === "CREATOR")
        : null;

    // filter out members already added
    const existingReviewerIds = new Set(reviewers.map((r) => r.user?.id));
    const existingAssigneeIds = new Set(assignees.map((a) => a.user?.id));
    const eligibleReviewers = (members || []).filter(
        (m) => REVIEWER_ELIGIBLE_ROLES.includes(m.role) && !existingReviewerIds.has(m.user?.id)
    );
    const eligibleAssignees = (members || []).filter(
        (m) => ASSIGNEE_ELIGIBLE_ROLES.includes(m.role) && !existingAssigneeIds.has(m.user?.id)
    );

    // refresh the members list so the pickers stay current
    useEffect(() => {
        if (groupId) refreshActiveGroup();
    }, [groupId]);

    // auto-redirect to dashboard if the user loses access to this group
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

    // re-fetch task data (called after errors to fix stale UI)
    function refetchTask() {
        if (!groupId || !taskId) return;
        apiGet(`/api/groups/${groupId}/task/${taskId}`)
            .then((data) => {
                setTask(data);
                setEditTitle(data.title || "");
                setEditDesc(data.description || "");
                setEditState(data.taskState || "TODO");
                lastFetchRef.current = new Date().toISOString();
                resetTaskPoll();
            })
            .catch(() => {});
    }

    // quick heal - re-pull both group + task so everything's fresh
    function autoHeal() {
        refreshActiveGroup();
        refetchTask();
    }

    // load task on mount
    useEffect(() => {
        if (!groupId || !taskId) return;
        setLoading(true);
        setError(null);
        apiGet(`/api/groups/${groupId}/task/${taskId}`)
            .then((data) => {
                setTask(data);
                setEditTitle(data.title || "");
                setEditDesc(data.description || "");
                setEditState(data.taskState || "TODO");
                lastFetchRef.current = new Date().toISOString();
                resetTaskPoll();
            })
            .catch(() => setError("Failed to load task"))
            .finally(() => setLoading(false));
    }, [groupId, taskId]);

    async function handleSave() {
        try {
            const body = {};
            if (editTitle !== task.title) body.title = editTitle;
            if (editDesc !== task.description) body.description = editDesc;
            if (editState !== task.taskState) body.taskState = editState;
            const updated = await apiPatch(`/api/groups/${groupId}/task/${taskId}`, body);
            setTask(updated);
            setEditing(false);
        } catch (err) {
            showToast(err?.message || "Failed to save changes");
            autoHeal();
        }
    }

    async function handleStateChange(newState) {
        try {
            const updated = await apiPatch(`/api/groups/${groupId}/task/${taskId}`, {
                taskState: newState,
            });
            setTask(updated);
            setEditState(newState);
        } catch (err) {
            showToast(err?.message || "Failed to change state");
            autoHeal();
        }
    }

    async function handleReview() {
        setSubmittingReview(true);
        try {
            const updated = await apiPost(`/api/groups/${groupId}/task/${taskId}/review`, {
                reviewComment,
                reviewersDecision: reviewDecision,
            });
            setTask(updated);
            setReviewComment("");
        } catch (err) {
            showToast(err?.message || "Failed to submit review");
            autoHeal();
        } finally {
            setSubmittingReview(false);
        }
    }

    async function handleAddParticipant(userId, role) {
        try {
            const updated = await apiPost(
                `/api/groups/${groupId}/task/${taskId}/taskParticipants`,
                { userId, taskParticipantRole: role }
            );
            setTask(updated);
            if (role === "REVIEWER") setShowReviewerPicker(false);
            else setShowAssigneePicker(false);
        } catch (err) {
            showToast(err?.message || "Failed to add participant");
            autoHeal();
        }
    }

    async function handleMoveToBeReviewed() {
        try {
            const updated = await apiPost(`/api/groups/${groupId}/task/${taskId}/to-be-reviewed`);
            setTask(updated);
        } catch (err) {
            showToast(err?.message || "Failed to move task to review");
            autoHeal();
        }
    }

    /* upload a single file (task or assignee) */
    async function uploadSingleFile(file, isAssignee) {
        const currentCount = isAssignee
            ? (task?.assigneeFiles?.length || 0)
            : (task?.files?.length || 0);
        const maxFiles = isAssignee ? LIMITS.MAX_ASSIGNEE_FILES : LIMITS.MAX_TASK_FILES;
        if (currentCount >= maxFiles) {
            showToast(`Maximum ${maxFiles} files already uploaded`);
            return;
        }
        if (isFileTooLarge(file)) {
            showToast(`File exceeds ${LIMITS.MAX_FILE_SIZE_MB} MB limit`);
            return;
        }
        const fd = new FormData();
        fd.append("file", file);
        const endpoint = isAssignee
            ? `/api/groups/${groupId}/task/${taskId}/assignee-files`
            : `/api/groups/${groupId}/task/${taskId}/files`;
        try {
            const updated = await apiPost(endpoint, fd);
            setTask(updated);
        } catch (err) {
            showToast(err?.message || `Failed to upload ${isAssignee ? "assignee " : ""}file`);
            autoHeal();
        }
    }

    async function handleFileUpload(e) {
        const file = e.target.files[0];
        if (!file) return;
        await uploadSingleFile(file, false);
    }

    async function handleAssigneeFileUpload(e) {
        const file = e.target.files[0];
        if (!file) return;
        await uploadSingleFile(file, true);
    }

    function handleFileDrop(e, isAssignee) {
        e.preventDefault();
        if (isAssignee) setAssigneeDragOver(false);
        else setFileDragOver(false);
        const file = e.dataTransfer.files?.[0];
        if (!file) return;
        uploadSingleFile(file, isAssignee);
    }

    async function handleDownload(fileId, filename, isAssignee) {
        setDownloadingId(fileId);
        try {
            const endpoint = isAssignee
                ? `/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}/download`
                : `/api/groups/${groupId}/task/${taskId}/files/${fileId}/download`;
            const blob = await apiGet(endpoint, { responseType: "blob" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = filename;
            a.click();
            URL.revokeObjectURL(url);
        } catch (err) {
            showToast(err?.message || "Failed to download file");
        } finally {
            setDownloadingId(null);
        }
    }

    async function handleDeleteFile(fileId, isAssignee) {
        try {
            const endpoint = isAssignee
                ? `/api/groups/${groupId}/task/${taskId}/assignee-files/${fileId}`
                : `/api/groups/${groupId}/task/${taskId}/files/${fileId}`;
            await apiDelete(endpoint);
            // Refetch task
            const refreshed = await apiGet(`/api/groups/${groupId}/task/${taskId}`);
            setTask(refreshed);
        } catch (err) {
            showToast(err?.message || "Failed to delete file");
            autoHeal();
        }
    }

    if (loading) return <Spinner />;
    if (error) return <div className="task-page-error">{error}</div>;
    if (!task) return <div className="task-page-error">Task not found</div>;

    const groupName = activeGroup?.name || `Group ${groupId}`;

    return (
        <div className="task-page">

            <div className={`task-main${rightOpen ? "" : " full-width"}`}>
                {/* Breadcrumb / top bar */}
                <div className="task-breadcrumb">
                    <Link to="/dashboard" className="task-breadcrumb-back" title="Back to group">
                        <FiArrowLeft size={14} />
                        <span>Back to group</span>
                    </Link>
                    <span className="task-breadcrumb-trail">
                        <Link to="/dashboard" className="breadcrumb-link" title={groupName}>{groupName}</Link>
                        <FiChevronRight size={12} className="breadcrumb-sep" />
                        <span className="breadcrumb-current">Task</span>
                    </span>

                    <div className="task-breadcrumb-right">
                        <span className="task-meta-byline">
                            By: {creator?.user?.name || creator?.user?.email || "—"}
                        </span>
                        {canEdit && !editing && (
                            <button className="task-edit-btn" onClick={() => setEditing(true)} title="Edit task">
                                <FiEdit2 size={14} /> Edit
                            </button>
                        )}
                        <span className="task-meta-group" title={groupName}>Group: {groupName}</span>
                        {task.lastEditBy && (
                            <span className="task-meta-lastedit">
                                LastEditBy: {task.lastEditBy.name || task.lastEditBy.email}
                            </span>
                        )}
                        <Link
                            to={`/group/${groupId}/task/${taskId}/comments`}
                            className="task-chat-btn"
                            title="Comments"
                        >
                            <FiMessageCircle size={22} />
                        </Link>
                    </div>
                </div>

                {/* Poll-detected changes banner */}
                {(taskHasChanged || taskIsStale) && (
                    <div className="task-stale-banner">
                        <span>{taskHasChanged ? "This task has been updated." : "Data may be outdated."}</span>
                        <button className="stale-refresh-btn" onClick={refetchTask}>
                            <FiRefreshCw size={14} className="stale-refresh-icon" /> Refresh
                        </button>
                    </div>
                )}

                {/* State selector */}
                <div className="task-state-bar">
                    {canChangeState || canEdit ? (
                        <select
                            className="task-state-select"
                            value={editing ? editState : task.taskState}
                            onChange={(e) => {
                                if (editing) {
                                    setEditState(e.target.value);
                                } else {
                                    handleStateChange(e.target.value);
                                }
                            }}
                        >
                            {STATE_OPTIONS.map((s) => (
                                <option key={s} value={s}>
                                    {STATE_LABELS[s]}
                                </option>
                            ))}
                        </select>
                    ) : (
                        <span className="task-state-badge">
                            {STATE_LABELS[task.taskState] || task.taskState}
                        </span>
                    )}
                </div>

                {/* Title & description */}
                <div className="task-body">
                    {editing ? (
                        <>
                            <input
                                className="task-title-input"
                                value={editTitle}
                                onChange={(e) => setEditTitle(e.target.value)}
                                placeholder="Task title"
                                maxLength={LIMITS.TASK_TITLE}
                            />
                            <span className="char-count">{editTitle.length}/{LIMITS.TASK_TITLE}</span>
                            <textarea
                                className="task-desc-input"
                                value={editDesc}
                                onChange={(e) => setEditDesc(e.target.value)}
                                placeholder="Task description"
                                rows={6}
                                maxLength={LIMITS.TASK_DESCRIPTION}
                            />
                            <span className="char-count">{editDesc.length}/{LIMITS.TASK_DESCRIPTION}</span>
                            <div className="task-edit-actions">
                                <button className="btn-primary" onClick={handleSave}>
                                    Save
                                </button>
                                <button className="btn-secondary" onClick={() => setEditing(false)}>
                                    Cancel
                                </button>
                            </div>
                        </>
                    ) : (
                        <>
                            <h1 className="task-title">{task.title || "Untitled"}</h1>
                            <p className="task-description">
                                {task.description || <em>No description</em>}
                            </p>
                        </>
                    )}

                    {/* Files */}
                    <div
                        className={`task-files-section${fileDragOver ? " drag-over" : ""}`}
                        onDragOver={(e) => { if (canUploadFiles) { e.preventDefault(); setFileDragOver(true); } }}
                        onDragLeave={() => setFileDragOver(false)}
                        onDrop={(e) => { if (canUploadFiles) handleFileDrop(e, false); }}
                    >
                        <h3>
                            Files ({task.files?.length || 0}/{LIMITS.MAX_TASK_FILES}):
                            {canUploadFiles && (task.files?.length || 0) < LIMITS.MAX_TASK_FILES && (
                                <button
                                    className="task-file-add"
                                    onClick={() => fileInputRef.current?.click()}
                                    title="Upload file"
                                >
                                    <FiPlus size={12} />
                                </button>
                            )}
                        </h3>
                        <input
                            ref={fileInputRef}
                            type="file"
                            hidden
                            onChange={handleFileUpload}
                        />
                        {task.files && task.files.length > 0 ? (
                            <ul className="task-file-list">
                                {[...task.files].map((f) => {
                                    const Icon = getFileIcon(f.name);
                                    return (
                                        <li key={f.id} className="task-file-item">
                                            <button
                                                className="task-file-icon-btn"
                                                title="Download"
                                                onClick={() => handleDownload(f.id, f.name, false)}
                                            >
                                                <Icon size={16} />
                                                <span className="task-file-dl-overlay">
                                                    <FiDownload size={9} />
                                                </span>
                                            </button>
                                            <span
                                                className="task-file-name"
                                                title={f.name}
                                                role="button"
                                                tabIndex={0}
                                                onClick={() => handleDownload(f.id, f.name, false)}
                                                onKeyDown={(e) => { if (e.key === "Enter") handleDownload(f.id, f.name, false); }}
                                            >
                                                {f.name}
                                            </span>
                                            {downloadingId === f.id && <span className="task-file-downloading">↓</span>}
                                            {canUploadFiles && (
                                                <button
                                                    className="task-file-rm"
                                                    title="Remove"
                                                    onClick={() => handleDeleteFile(f.id, false)}
                                                >
                                                    <FiTrash2 size={13} />
                                                </button>
                                            )}
                                        </li>
                                    );
                                })}
                            </ul>
                        ) : (
                            <p className="task-no-files">No files</p>
                        )}
                        {canUploadFiles && (task.files?.length || 0) < LIMITS.MAX_TASK_FILES && (
                            <span className="task-drop-hint">Drop a file here to upload</span>
                        )}
                    </div>
                </div>
            </div>


            <aside className={`task-right-sidebar${rightOpen ? "" : " collapsed"}`}>
                <button
                    className="task-right-toggle"
                    onClick={() => setRightOpen((v) => !v)}
                    title={rightOpen ? "Hide panel" : "Show panel"}
                >
                    {rightOpen ? <FiChevronRight size={14} /> : <FiChevronLeft size={14} />}
                </button>

                {rightOpen && (
                    <div className="task-right-content">
                        {/* reviewers */}
                        <div className="task-sidebar-section">
                            <h4>
                                Reviewer
                                {canAddParticipants && (
                                    <button
                                        className="task-sidebar-add"
                                        title="Add reviewer"
                                        onClick={() => { setShowReviewerPicker((v) => !v); setShowAssigneePicker(false); }}
                                    >
                                        <FiPlus size={12} />
                                    </button>
                                )}
                            </h4>

                            {/* Reviewer picker dropdown */}
                            {showReviewerPicker && canAddParticipants && (
                                <div className="task-participant-picker">
                                    {eligibleReviewers.length === 0 ? (
                                        <span className="task-participant-picker-empty">No eligible members</span>
                                    ) : (
                                        eligibleReviewers.map((m) => (
                                            <div
                                                key={m.user?.id}
                                                className="task-participant-picker-item"
                                                onClick={() => handleAddParticipant(m.user?.id, "REVIEWER")}
                                            >
                                                <img src={userImg(m.user)} alt="" className="task-participant-img" />
                                                <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                                <span className="task-participant-role-tag">{m.role.replace("_", " ")}</span>
                                            </div>
                                        ))
                                    )}
                                </div>
                            )}

                            <div className="task-participant-list">
                                {reviewers.length === 0 ? (
                                    <span className="muted">No reviewer</span>
                                ) : (
                                    reviewers.map((r) => (
                                        <div key={r.id} className="task-participant-row">
                                            <img
                                                src={userImg(r.user)}
                                                alt=""
                                                className="task-participant-img"
                                            />
                                            <span title={r.user?.email}>{r.user?.name || r.user?.email}</span>
                                        </div>
                                    ))
                                )}
                            </div>

                            {/* Review info (read-only) */}
                            <div className="task-review-info">
                                <div className="task-info-row">
                                    <span className="task-info-label">Latest review by:</span>
                                    <span>{task.reviewedBy?.name || task.reviewedBy?.email || "—"}</span>
                                </div>
                                <div className="task-info-row">
                                    <span className="task-info-label">Latest review comment:</span>
                                    <span>{task.reviewComment || "—"}</span>
                                </div>
                                <div className="task-info-row">
                                    <span className="task-info-label">Decision:</span>
                                    <span className={`task-decision ${task.reviewersDecision === "APPROVE" ? "approved" : task.reviewersDecision === "REJECT" ? "rejected" : ""}`}>
                                        {task.reviewersDecision || "—"}
                                    </span>
                                </div>
                            </div>

                            {/* Reviewer form — only for reviewers / managers */}
                            {canReview && (
                                <div className="task-review-form">
                                    <label>Reviewer Comment</label>
                                    <div className="task-review-textarea-wrapper">
                                        <textarea
                                            className="task-review-textarea"
                                            value={reviewComment}
                                            onChange={(e) =>
                                                setReviewComment(e.target.value.slice(0, LIMITS.TASK_REVIEW_COMMENT))
                                            }
                                            placeholder="Your review comment…"
                                            rows={3}
                                            maxLength={LIMITS.TASK_REVIEW_COMMENT}
                                        />
                                        <span className="task-review-counter">
                                            {reviewComment.length}/{LIMITS.TASK_REVIEW_COMMENT}
                                        </span>
                                    </div>
                                    <div className="task-review-actions">
                                        <label>Decision:</label>
                                        <select
                                            value={reviewDecision}
                                            onChange={(e) => setReviewDecision(e.target.value)}
                                            className="task-review-decision"
                                        >
                                            {DECISION_OPTIONS.map((d) => (
                                                <option key={d} value={d}>
                                                    {d}
                                                </option>
                                            ))}
                                        </select>
                                        <button
                                            className="btn-primary btn-sm"
                                            disabled={submittingReview}
                                            onClick={handleReview}
                                        >
                                            Confirm
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* assignees */}
                        <div className="task-sidebar-section">
                            <h4>
                                Assignees
                                {canAddParticipants && (
                                    <button
                                        className="task-sidebar-add"
                                        title="Add assignee"
                                        onClick={() => { setShowAssigneePicker((v) => !v); setShowReviewerPicker(false); }}
                                    >
                                        <FiPlus size={12} />
                                    </button>
                                )}
                            </h4>

                            {/* Assignee picker dropdown */}
                            {showAssigneePicker && canAddParticipants && (
                                <div className="task-participant-picker">
                                    {eligibleAssignees.length === 0 ? (
                                        <span className="task-participant-picker-empty">No eligible members</span>
                                    ) : (
                                        eligibleAssignees.map((m) => (
                                            <div
                                                key={m.user?.id}
                                                className="task-participant-picker-item"
                                                onClick={() => handleAddParticipant(m.user?.id, "ASSIGNEE")}
                                            >
                                                <img src={userImg(m.user)} alt="" className="task-participant-img" />
                                                <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                                <span className="task-participant-role-tag">{m.role.replace("_", " ")}</span>
                                            </div>
                                        ))
                                    )}
                                </div>
                            )}

                            <div className="task-participant-list">
                                {assignees.length === 0 ? (
                                    <span className="muted">No assignees</span>
                                ) : (
                                    assignees.map((a) => (
                                        <div key={a.id} className="task-participant-row">
                                            <img
                                                src={userImg(a.user)}
                                                alt=""
                                                className="task-participant-img"
                                            />
                                            <span title={a.user?.email}>{a.user?.name || a.user?.email}</span>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>

                        {/* assignee files */}
                        <div
                            className={`task-sidebar-section${assigneeDragOver ? " drag-over" : ""}`}
                            onDragOver={(e) => { if (canUploadAssigneeFiles) { e.preventDefault(); setAssigneeDragOver(true); } }}
                            onDragLeave={() => setAssigneeDragOver(false)}
                            onDrop={(e) => { if (canUploadAssigneeFiles) handleFileDrop(e, true); }}
                        >
                            <h3>
                                Assignee Files ({task.assigneeFiles?.length || 0}/{LIMITS.MAX_ASSIGNEE_FILES})
                                {canUploadAssigneeFiles && (task.assigneeFiles?.length || 0) < LIMITS.MAX_ASSIGNEE_FILES && (
                                    <button
                                        className="task-file-add"
                                        onClick={() => assigneeFileInputRef.current?.click()}
                                        title="Upload assignee file"
                                    >
                                        <FiPlus size={12} />
                                    </button>
                                )}
                            </h3>
                            <input
                                ref={assigneeFileInputRef}
                                type="file"
                                hidden
                                onChange={handleAssigneeFileUpload}
                            />
                            {task.assigneeFiles && task.assigneeFiles.length > 0 ? (
                                <ul className="task-file-list">
                                    {[...task.assigneeFiles].map((f) => {
                                        const Icon = getFileIcon(f.name);
                                        return (
                                            <li key={f.id} className="task-file-item">
                                                <button
                                                    className="task-file-icon-btn"
                                                    title="Download"
                                                    onClick={() => handleDownload(f.id, f.name, true)}
                                                >
                                                    <Icon size={16} />
                                                    <span className="task-file-dl-overlay">
                                                        <FiDownload size={9} />
                                                    </span>
                                                </button>
                                                <span
                                                    className="task-file-name"
                                                    title={f.name}
                                                    role="button"
                                                    tabIndex={0}
                                                    onClick={() => handleDownload(f.id, f.name, true)}
                                                    onKeyDown={(e) => { if (e.key === "Enter") handleDownload(f.id, f.name, true); }}
                                                >
                                                    {f.name}
                                                </span>
                                                {downloadingId === f.id && <span className="task-file-downloading">↓</span>}
                                                {canUploadAssigneeFiles && (
                                                    <button
                                                        className="task-file-rm"
                                                        title="Remove"
                                                        onClick={() => handleDeleteFile(f.id, true)}
                                                    >
                                                        <FiTrash2 size={13} />
                                                    </button>
                                                )}
                                            </li>
                                        );
                                    })}
                                </ul>
                            ) : (
                                <p className="task-no-files">No assignee files</p>
                            )}
                            {canUploadAssigneeFiles && (task.assigneeFiles?.length || 0) < LIMITS.MAX_ASSIGNEE_FILES && (
                                <span className="task-drop-hint">Drop a file here to upload</span>
                            )}
                        </div>

                        {/* Move to be reviewed */}
                        {canMoveToBeReviewed && task.taskState !== "TO_BE_REVIEWED" && task.taskState !== "DONE" && (
                            <div className="task-sidebar-section task-move-section">
                                <button className="btn-primary btn-block" onClick={handleMoveToBeReviewed}>
                                    Move to be reviewed
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </aside>
        </div>
    );
}



function userImg(u) {
    if (!u) return "";
    return u.imgUrl ? blobBase + u.imgUrl : u.defaultImgUrl ? blobBase + u.defaultImgUrl : "";
}