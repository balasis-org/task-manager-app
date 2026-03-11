import { useState, useEffect, useContext, useRef, useCallback } from "react";
import { useParams, Link, useSearchParams, useNavigate } from "react-router-dom";
import {
    FiArrowLeft,
    FiChevronRight,
    FiRefreshCw,
} from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { GroupContext } from "@context/GroupContext";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPost, apiPatch, apiDelete } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import useSmartPoll from "@hooks/useSmartPoll";
import { useBlobUrl } from "@context/BlobSasContext";
import { formatDateTime } from "@assets/js/formatDate";
import Spinner from "@components/Spinner";
import CommentCard from "@components/comments/CommentCard";
import CommentComposer from "@components/comments/CommentComposer";
import CommentDeleteModal from "@components/comments/CommentDeleteModal";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Comments.css";

const MAX_LEN = LIMITS.COMMENT;
const PAGE_SIZE = 5;

export default function Comments() {
    const { groupId, taskId } = useParams();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const { user } = useContext(AuthContext);
    const { activeGroup, myRole, refreshActiveGroup, presenceUserIds } = useContext(GroupContext);
    const showToast = useToast();
    const blobUrl = useBlobUrl();

    usePageTitle("Comments");

    const urlPage = searchParams.get("page");

    const [comments, setComments] = useState([]);
    const [task, setTask] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalPages, setTotalPages] = useState(0);
    const [refreshKey, setRefreshKey] = useState(0);

    const [newComment, setNewComment] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [showEmojis, setShowEmojis] = useState(false);
    const textareaRef = useRef(null);

    const [editingId, setEditingId] = useState(null);
    const [editText, setEditText] = useState("");

    const [deleteId, setDeleteId] = useState(null);

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

    const isLeaderOrManager = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";

    const participantRoleMap = {};
    if (task?.taskParticipants) {
        for (const tp of task.taskParticipants) {
            if (tp.user?.id) participantRoleMap[tp.user.id] = tp.taskParticipantRole;
        }
    }

    const isParticipant = user?.id && participantRoleMap[user.id];
    const canComment = isLeaderOrManager || !!isParticipant;

    const currentPage = urlPage ? Number(urlPage) : 1;

    /* -- sync group context with URL groupId (mirror of Task.jsx) -- */
    useEffect(() => {
        if (groupId) refreshActiveGroup();
    }, [groupId]);

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

    function goToPage(p) {
        setSearchParams({ page: String(p) });
    }

    useEffect(() => {
        if (!groupId || !taskId) return;
        apiGet(`/api/groups/${groupId}/task/${taskId}`)
            .then(setTask)
            .catch(() => {});
    }, [groupId, taskId]);

    useEffect(() => {
        if (!groupId || !taskId || urlPage !== null) return;
        apiGet(`/api/groups/${groupId}/task/${taskId}/comments?page=0&size=${PAGE_SIZE}&sort=createdAt,asc`)
            .then((data) => {
                const lastPage = Math.max(1, data.totalPages || 1);
                setSearchParams({ page: String(lastPage) }, { replace: true });
            })
            .catch(() => setError("Failed to load comments"));
    }, [groupId, taskId, urlPage]);

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
            setShowEmojis(false);
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
        if (editingId) {
            setEditText((prev) => (prev + em).slice(0, MAX_LEN));
            setShowEmojis(false);
        } else {
            setNewComment((prev) => (prev + em).slice(0, MAX_LEN));
            setShowEmojis(false);
            textareaRef.current?.focus();
        }
    }

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

            {(commentsHaveChanged || commentsStale) && (
                <div className="comments-stale-banner">
                    <span>{commentsHaveChanged ? "New comments available." : "Data may be outdated."}</span>
                    <button className="stale-refresh-btn" onClick={() => setRefreshKey(k => k + 1)}>
                        <FiRefreshCw size={14} className="stale-refresh-icon" /> Refresh
                    </button>
                </div>
            )}

            {totalPages > 1 && (
                <div className="comments-pagination">
                    {currentPage > 1 && (
                        <button
                            className="btn-secondary btn-sm"
                            onClick={() => goToPage(currentPage - 1)}
                        >
                            ← Prev
                        </button>
                    )}
                    <span className="comments-page-info">
                        {currentPage} / {totalPages}
                    </span>
                    {currentPage < totalPages && (
                        <button
                            className="btn-secondary btn-sm"
                            onClick={() => goToPage(currentPage + 1)}
                        >
                            Next →
                        </button>
                    )}
                </div>
            )}

            <div className="comments-list">
                {comments.length === 0 ? (
                    <div className="comments-empty">
                        <p>No comments yet</p>
                    </div>
                ) : (
                    comments.map((c) => (
                        <CommentCard
                            key={c.id}
                            comment={c}
                            isEditing={editingId === c.id}
                            editText={editText}
                            onEditTextChange={setEditText}
                            participantRoleMap={participantRoleMap}
                            canEdit={canEditComment(c)}
                            canDelete={canDeleteComment(c)}
                            onStartEdit={startEdit}
                            onCancelEdit={cancelEdit}
                            onSaveEdit={saveEdit}
                            onRequestDelete={setDeleteId}
                            blobUrl={blobUrl}
                            maxLen={MAX_LEN}
                            formatDateTime={formatDateTime}
                            presenceUserIds={presenceUserIds}
                        />
                    ))
                )}
            </div>

            {canComment && (
                <CommentComposer
                    newComment={newComment}
                    onNewCommentChange={setNewComment}
                    onSubmit={handleSubmit}
                    submitting={submitting}
                    showEmojis={showEmojis}
                    onToggleEmojis={() => setShowEmojis((v) => !v)}
                    onInsertEmoji={insertEmoji}
                    onClear={() => { setNewComment(""); setShowEmojis(false); }}
                    textareaRef={textareaRef}
                    maxLen={MAX_LEN}
                />
            )}

            {deleteId && (
                <CommentDeleteModal
                    onConfirm={confirmDelete}
                    onCancel={() => setDeleteId(null)}
                />
            )}
        </div>
    );
}
