import { useState, useEffect, useContext, useRef, useCallback } from "react";
import { useParams, Link, useSearchParams, useNavigate } from "react-router-dom";
import {
    FiArrowLeft,
    FiChevronRight,
    FiPlus,
    FiSmile,
    FiX,
    FiRefreshCw,
} from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { GroupContext } from "@context/GroupContext";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPost, apiPatch, apiDelete } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import useSmartPoll from "@hooks/useSmartPoll";
import blobBase from "@blobBase";
import Spinner from "@components/Spinner";
import "@styles/pages/Comments.css";

const EMOJIS = ["😀","😂","😍","👍","👎","🎉","🔥","❤️","😢","😮","🤔","😎","💯","✅","❌","⚡"];

const MAX_LEN = LIMITS.COMMENT;
const PAGE_SIZE = 5;

function userImg(u) {
    if (!u) return "";
    return u.imgUrl ? blobBase + u.imgUrl : u.defaultImgUrl ? blobBase + u.defaultImgUrl : "";
}

function formatDate(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export default function Comments() {
    const { groupId, taskId } = useParams();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const { user } = useContext(AuthContext);
    const { activeGroup, myRole, refreshActiveGroup } = useContext(GroupContext);
    const showToast = useToast();

    // URL-driven page (1-indexed in URL, 0-indexed for API)
    const urlPage = searchParams.get("page"); // string or null

    const [comments, setComments] = useState([]);
    const [task, setTask] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalPages, setTotalPages] = useState(0);
    const [refreshKey, setRefreshKey] = useState(0);

    // New comment input
    const [composerOpen, setComposerOpen] = useState(false);
    const [newComment, setNewComment] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [showEmojis, setShowEmojis] = useState(false);
    const textareaRef = useRef(null);

    // Edit state
    const [editingId, setEditingId] = useState(null);
    const [editText, setEditText] = useState("");

    // Delete confirm
    const [deleteId, setDeleteId] = useState(null);

    // lightweight comments-change poll
    const lastFetchRef = useRef(new Date().toISOString());

    const commentsPollCheck = useCallback(async () => {
        if (!groupId || !taskId) return;
        await apiGet(`/api/groups/${groupId}/task/${taskId}/comments/has-changed?since=${encodeURIComponent(lastFetchRef.current)}`);
    }, [groupId, taskId]);

    const {
        hasChanged: commentsHaveChanged,
        isStale: commentsStale,
        reset: resetCommentsPoll,
    } = useSmartPoll(commentsPollCheck, { enabled: !!groupId && !!taskId });

    // Derived roles
    const isLeaderOrManager = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";

    // build a map: userId -> taskParticipantRole for display
    const participantRoleMap = {};
    if (task?.taskParticipants) {
        for (const tp of task.taskParticipants) {
            if (tp.user?.id) participantRoleMap[tp.user.id] = tp.taskParticipantRole;
        }
    }

    // Can current user comment? (leader/manager or task participant)
    const isParticipant = user?.id && participantRoleMap[user.id];
    const canComment = isLeaderOrManager || !!isParticipant;

    const currentPage = urlPage ? Number(urlPage) : 1; // 1-indexed

    function goToPage(p) {
        setSearchParams({ page: String(p) });
    }

    useEffect(() => { // grab task info
        if (!groupId || !taskId) return;
        apiGet(`/api/groups/${groupId}/task/${taskId}`)
            .then(setTask)
            .catch(() => {});
    }, [groupId, taskId]);

    // if no ?page in URL, jump to last page
    useEffect(() => {
        if (!groupId || !taskId || urlPage !== null) return;
        apiGet(`/api/groups/${groupId}/task/${taskId}/comments?page=0&size=${PAGE_SIZE}&sort=createdAt,asc`)
            .then((data) => {
                const lastPage = Math.max(1, data.totalPages || 1);
                setSearchParams({ page: String(lastPage) }, { replace: true });
            })
            .catch(() => setError("Failed to load comments"));
    }, [groupId, taskId, urlPage]);

    // fetch comments when page changes
    useEffect(() => {
        if (!groupId || !taskId || urlPage === null) return;
        const apiPage = Math.max(0, Number(urlPage) - 1);
        setLoading(true);
        setError(null);
        apiGet(`/api/groups/${groupId}/task/${taskId}/comments?page=${apiPage}&size=${PAGE_SIZE}&sort=createdAt,asc`)
            .then((data) => {
                setComments(data.content || []);
                setTotalPages(data.totalPages || 0);
                lastFetchRef.current = new Date().toISOString();
                resetCommentsPoll();
            })
            .catch(() => setError("Failed to load comments"))
            .finally(() => setLoading(false));
    }, [groupId, taskId, urlPage, refreshKey]);

    async function handleSubmit() {
        if (!newComment.trim()) return;
        setSubmitting(true);
        try {
            await apiPost(
                `/api/groups/${groupId}/task/${taskId}/comments`,
                { comment: newComment.trim() }
            );
            setNewComment("");
            setComposerOpen(false);
            // Navigate to last page (new comment lands there in asc order)
            // clear page param so probe effect resolves to last page
            setSearchParams({}, { replace: true });
        } catch (err) {
            showToast(err?.message || "Failed to add comment");
            refreshActiveGroup();
        } finally {
            setSubmitting(false);
        }
    }

    function startEdit(c) {
        setEditingId(c.id);
        setEditText(c.comment);
    }
    function cancelEdit() {
        setEditingId(null);
        setEditText("");
    }
    async function saveEdit(commentId) {
        try {
            const updated = await apiPatch(
                `/api/groups/${groupId}/task/${taskId}/comments/${commentId}`,
                { comment: editText.trim() }
            );
            setComments((prev) =>
                prev.map((c) => (c.id === commentId ? updated : c))
            );
            setEditingId(null);
            setEditText("");
        } catch (err) {
            showToast(err?.message || "Failed to save comment");
            refreshActiveGroup();
        }
    }

    async function confirmDelete() {
        if (!deleteId) return;
        try {
            await apiDelete(
                `/api/groups/${groupId}/task/${taskId}/comments/${deleteId}`
            );
            setDeleteId(null);
            // If last comment on this page and not page 1, go to previous page
            if (comments.length <= 1 && currentPage > 1) {
                goToPage(currentPage - 1);
            } else {
                setRefreshKey((k) => k + 1);
            }
        } catch (err) {
            showToast(err?.message || "Failed to delete comment");
            refreshActiveGroup();
        }
    }

    function insertEmoji(em) {
        if (composerOpen) {
            setNewComment((prev) => (prev + em).slice(0, MAX_LEN));
            setShowEmojis(false);
            textareaRef.current?.focus();
        } else if (editingId) {
            setEditText((prev) => (prev + em).slice(0, MAX_LEN));
            setShowEmojis(false);
        }
    }

    // Permissions per comment
    function canEditComment(c) {
        return c.creator?.id === user?.id;
    }
    function canDeleteComment(c) {
        return c.creator?.id === user?.id || isLeaderOrManager;
    }

    const groupName = activeGroup?.name || `Group ${groupId}`;

    if ((loading && urlPage !== null) || urlPage === null) return <Spinner />;
    if (error) return <div className="comments-page-error">{error}</div>;

    return (
        <div className="comments-page">
            {/* breadcrumb nav */}
            <div className="comments-breadcrumb">
                <button
                    onClick={() => navigate(-1)}
                    className="comments-back"
                    title="Go back"
                >
                    <FiArrowLeft size={14} />
                    <span>Go back</span>
                </button>
                <span className="comments-trail">
                    <Link to="/dashboard" className="breadcrumb-link" title={groupName}>{groupName}</Link>
                    <FiChevronRight size={12} className="breadcrumb-sep" />
                    <Link to={`/group/${groupId}/task/${taskId}`} className="breadcrumb-link">Task</Link>
                    <FiChevronRight size={12} className="breadcrumb-sep" />
                    <span className="breadcrumb-current">Comments</span>
                </span>
            </div>

            {/* Poll-detected new comments banner */}
            {(commentsHaveChanged || commentsStale) && (
                <div className="comments-stale-banner">
                    <span>{commentsHaveChanged ? "New comments available." : "Data may be outdated."}</span>
                    <button className="stale-refresh-btn" onClick={() => setRefreshKey(k => k + 1)}>
                        <FiRefreshCw size={14} className="stale-refresh-icon" /> Refresh
                    </button>
                </div>
            )}

            {/* add button (when theres already comments) */}
            {canComment && comments.length > 0 && (
                <div className="comments-top-actions">
                    <button
                        className="btn-add-comment"
                        onClick={() => setComposerOpen((v) => !v)}
                    >
                        <FiPlus size={14} /> Add a comment
                    </button>
                </div>
            )}

            {/* pagination */}
            {totalPages > 1 && (
                <div className="comments-pagination">
                    <button
                        className="btn-secondary btn-sm"
                        disabled={currentPage <= 1}
                        onClick={() => goToPage(currentPage - 1)}
                    >
                        ← Prev
                    </button>
                    <span className="comments-page-info">
                        {currentPage} / {totalPages}
                    </span>
                    <button
                        className="btn-secondary btn-sm"
                        disabled={currentPage >= totalPages}
                        onClick={() => goToPage(currentPage + 1)}
                    >
                        Next →
                    </button>
                </div>
            )}


            <div className="comments-list">
                {comments.length === 0 ? (
                    <div className="comments-empty">
                        <p>No comments yet</p>
                        {canComment && (
                            <button
                                className="btn-add-comment centered"
                                onClick={() => setComposerOpen(true)}
                            >
                                <FiPlus size={14} /> Add a comment
                            </button>
                        )}
                    </div>
                ) : (
                    comments.map((c) => {
                        const isEditing = editingId === c.id;
                        const isDeletedCreator = !c.creator;
                        const taskRole = !isDeletedCreator ? participantRoleMap[c.creator?.id] : null;
                        return (
                            <div key={c.id} className="comment-card">
                                {isDeletedCreator ? (
                                    <div className="comment-avatar comment-avatar-deleted" title="Deleted user" />
                                ) : (
                                    <img
                                        src={userImg(c.creator)}
                                        alt=""
                                        className="comment-avatar"
                                    />
                                )}
                                <div className="comment-body">
                                    <div className="comment-header">
                                        <span className="comment-author">
                                            {isDeletedCreator
                                                ? <>{c.creatorNameSnapshot || "Unknown"} <span className="comment-deleted-tag">(Deleted User)</span></>
                                                : <>{c.creator?.name || c.creator?.email || "Unknown"}
                                                    {taskRole && (
                                                        <span className="comment-role-tag">
                                                            ({taskRole.replace("_", " ")})
                                                        </span>
                                                    )}
                                                  </>
                                            }
                                        </span>
                                        <div className="comment-actions">
                                            {!isEditing && canEditComment(c) && !isDeletedCreator && (
                                                <button
                                                    className="comment-action-btn"
                                                    title="Edit"
                                                    onClick={() => startEdit(c)}
                                                >
                                                    Edit
                                                </button>
                                            )}
                                            {!isEditing && canDeleteComment(c) && (
                                                <button
                                                    className="comment-action-btn danger"
                                                    title="Delete"
                                                    onClick={() => setDeleteId(c.id)}
                                                >
                                                    Delete
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {isEditing ? (
                                        <div className="comment-edit-area">
                                            <textarea
                                                className="comment-edit-textarea"
                                                value={editText}
                                                onChange={(e) =>
                                                    setEditText(e.target.value.slice(0, MAX_LEN))
                                                }
                                                rows={3}
                                                maxLength={MAX_LEN}
                                            />
                                            <div className="comment-edit-footer">
                                                <span className="comment-counter">
                                                    {editText.length}/{MAX_LEN}
                                                </span>
                                                <div className="comment-edit-btns">
                                                    <button
                                                        className="btn-cancel-edit"
                                                        onClick={cancelEdit}
                                                    >
                                                        Cancel edit
                                                    </button>
                                                    <button
                                                        className="btn-save-edit"
                                                        onClick={() => saveEdit(c.id)}
                                                    >
                                                        Save
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        <p className="comment-text">{c.comment}</p>
                                    )}

                                    <span className="comment-date">
                                        {formatDate(c.createdAt)}
                                    </span>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>

            {/* new comment composer - slides up from bottom */}
            <div className={`comments-composer${composerOpen ? " open" : ""}`}>
                <div className="composer-bar">
                    <span className="composer-label">Make a comment…</span>

                    {/* Emoji picker */}
                    <div className="composer-emoji-wrapper">
                        <button
                            className="composer-emoji-btn"
                            onClick={() => setShowEmojis((v) => !v)}
                            title="Emojis"
                        >
                            <FiSmile size={16} /> emojis here.
                        </button>
                        {showEmojis && (
                            <div className="composer-emoji-picker">
                                {EMOJIS.map((em) => (
                                    <span
                                        key={em}
                                        className="composer-emoji-item"
                                        onClick={() => insertEmoji(em)}
                                    >
                                        {em}
                                    </span>
                                ))}
                            </div>
                        )}
                    </div>

                    <span className="composer-hint">pasteable on click</span>

                    <button
                        className="composer-close-btn"
                        onClick={() => { setComposerOpen(false); setShowEmojis(false); }}
                        title="Close"
                    >
                        <FiX size={14} />
                    </button>
                </div>

                <div className="composer-input-row">
                    <textarea
                        ref={textareaRef}
                        className="composer-textarea"
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value.slice(0, MAX_LEN))}
                        placeholder="Write your comment…"
                        rows={2}
                        maxLength={MAX_LEN}
                    />
                    <span className="composer-counter">
                        {newComment.length}/{MAX_LEN}
                    </span>
                </div>

                <div className="composer-submit-row">
                    <button
                        className="btn-primary btn-sm"
                        disabled={submitting || !newComment.trim()}
                        onClick={handleSubmit}
                    >
                        submit
                    </button>
                </div>
            </div>

            {/* confirm delete popup */}
            {deleteId && (
                <div className="comments-overlay" onClick={() => setDeleteId(null)}>
                    <div
                        className="comments-confirm-popup"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <p>Are you sure you want to delete this comment?</p>
                        <div className="confirm-actions">
                            <button
                                className="btn-secondary"
                                onClick={() => setDeleteId(null)}
                            >
                                Cancel
                            </button>
                            <button className="btn-danger" onClick={confirmDelete}>
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
