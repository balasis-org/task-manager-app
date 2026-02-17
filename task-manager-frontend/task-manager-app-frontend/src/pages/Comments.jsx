import { useState, useEffect, useContext, useRef } from "react";
import { useParams, Link } from "react-router-dom";
import {
    FiArrowLeft,
    FiPlus,
    FiSmile,
    FiX,
} from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { GroupContext } from "@context/GroupContext";
import { apiGet, apiPost, apiPatch, apiDelete } from "@assets/js/apiClient.js";
import blobBase from "@blobBase";
import Spinner from "@components/Spinner";
import "@styles/pages/Comments.css";

const EMOJIS = ["😀","😂","😍","👍","👎","🎉","🔥","❤️","😢","😮","🤔","😎","💯","✅","❌","⚡"];

const MAX_LEN = 250;

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
    const { user } = useContext(AuthContext);
    const { activeGroup, myRole } = useContext(GroupContext);

    const [comments, setComments] = useState([]);
    const [task, setTask] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Pagination
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const PAGE_SIZE = 20;

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

    // Derived roles
    const isLeaderOrManager = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";

    // Build a map: userId → taskParticipantRole for display
    const participantRoleMap = {};
    if (task?.taskParticipants) {
        for (const tp of task.taskParticipants) {
            if (tp.user?.id) participantRoleMap[tp.user.id] = tp.taskParticipantRole;
        }
    }

    // Can current user comment? (leader/manager or task participant)
    const isParticipant = user?.id && participantRoleMap[user.id];
    const canComment = isLeaderOrManager || !!isParticipant;

    // ── Fetch task + comments ──
    useEffect(() => {
        if (!groupId || !taskId) return;
        setLoading(true);
        setError(null);
        Promise.all([
            apiGet(`/api/groups/${groupId}/task/${taskId}`),
            apiGet(`/api/groups/${groupId}/task/${taskId}/comments?page=${page}&size=${PAGE_SIZE}&sort=createdAt,desc`),
        ])
            .then(([taskData, commentsData]) => {
                setTask(taskData);
                setComments(commentsData.content || []);
                setTotalPages(commentsData.totalPages || 0);
            })
            .catch(() => setError("Failed to load comments"))
            .finally(() => setLoading(false));
    }, [groupId, taskId, page]);

    // ── Add comment ──
    async function handleSubmit() {
        if (!newComment.trim()) return;
        setSubmitting(true);
        try {
            const created = await apiPost(
                `/api/groups/${groupId}/task/${taskId}/comments`,
                { comment: newComment.trim() }
            );
            setComments((prev) => [created, ...prev]);
            setNewComment("");
            setComposerOpen(false);
        } catch {
            alert("Failed to add comment");
        } finally {
            setSubmitting(false);
        }
    }

    // ── Edit comment ──
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
        } catch {
            alert("Failed to save comment");
        }
    }

    // ── Delete comment ──
    async function confirmDelete() {
        if (!deleteId) return;
        try {
            await apiDelete(
                `/api/groups/${groupId}/task/${taskId}/comments/${deleteId}`
            );
            setComments((prev) => prev.filter((c) => c.id !== deleteId));
            setDeleteId(null);
        } catch {
            alert("Failed to delete comment");
        }
    }

    // ── Insert emoji ──
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

    if (loading) return <Spinner />;
    if (error) return <div className="comments-page-error">{error}</div>;

    return (
        <div className="comments-page">
            {/* ── Breadcrumb ── */}
            <div className="comments-breadcrumb">
                <Link
                    to={`/group/${groupId}/task/${taskId}`}
                    className="comments-back"
                    title="Back to task"
                >
                    <FiArrowLeft size={14} />
                    <span>Go back</span>
                </Link>
                <span className="comments-trail">
                    {groupName} → Task → comments
                </span>
            </div>

            {/* ── Top area: add comment button if comments exist ── */}
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

            {/* ── Comments list ── */}
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
                        const taskRole = participantRoleMap[c.creator?.id];
                        return (
                            <div key={c.id} className="comment-card">
                                <img
                                    src={userImg(c.creator)}
                                    alt=""
                                    className="comment-avatar"
                                />
                                <div className="comment-body">
                                    <div className="comment-header">
                                        <span className="comment-author">
                                            {c.creator?.name || c.creator?.email || "Unknown"}
                                            {taskRole && (
                                                <span className="comment-role-tag">
                                                    ({taskRole.replace("_", " ")})
                                                </span>
                                            )}
                                        </span>
                                        <div className="comment-actions">
                                            {!isEditing && canEditComment(c) && (
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

            {/* ── Pagination ── */}
            {totalPages > 1 && (
                <div className="comments-pagination">
                    <button
                        className="btn-secondary btn-sm"
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                    >
                        ← Prev
                    </button>
                    <span className="comments-page-info">
                        {page + 1} / {totalPages}
                    </span>
                    <button
                        className="btn-secondary btn-sm"
                        disabled={page >= totalPages - 1}
                        onClick={() => setPage((p) => p + 1)}
                    >
                        Next →
                    </button>
                </div>
            )}

            {/* ── Composer (slides in from bottom) ── */}
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

            {/* ── Delete confirmation popup ── */}
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