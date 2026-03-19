import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { FiMessageCircle, FiLock, FiTrash2 } from "react-icons/fi";
import { apiDelete } from "@assets/js/apiClient.js";
import { useToast } from "@context/ToastContext";
import { formatDate } from "@assets/js/formatDate";
import "@styles/dashboard/TaskTable.css";

// thresholds match the backend's priority scale (1-10).
// color-coded: 1-4 = Low (green), 5-7 = Med (yellow), 8-10 = High (red)
function priorityTag(p) {
    if (p == null) return "-";
    let label, cls;
    if (p <= 4)      { label = "L";  cls = "priority-low"; }
    else if (p <= 7) { label = "M";  cls = "priority-med"; }
    else             { label = "H";  cls = "priority-high"; }
    return <span className={`priority-tag ${cls}`}>{label}({p})</span>;
}

export default function TaskTable({ tasks, groupId, gridTemplate, visCols, canManageTasks, onDeleted, deleteColWidth = "42px" }) {
    const navigate = useNavigate();
    const showToast = useToast();
    const [flashId, setFlashId] = useState(null);
    const [confirmDeleteId, setConfirmDeleteId] = useState(null);
    const [deletingId, setDeletingId] = useState(null);

    const flashRow = useCallback((id) => {
        setFlashId(id);
        setTimeout(() => setFlashId(null), 500);
    }, []);

    if (!tasks || tasks.length === 0) {
        return <div className="task-table-empty">No tasks</div>;
    }

    /*  t.i  = task id          t.cn = creator name
        t.t  = title            t.nc = has new comments (bool)
        t.p  = priority (1-10)  t.cc = comment count
        t.dd = due date         t.dl = deletable (bool)
        t.a  = accessible (bool)
        - short keys from the backend list-tasks DTO to keep payloads small */

    const gridStyle = gridTemplate
        ? { gridTemplateColumns: canManageTasks ? gridTemplate + " " + deleteColWidth : gridTemplate }
        : undefined;

    const visSet = new Set(visCols || [0, 1, 2, 3, 4, 5]);

    function goToComments(e, taskId, accessible) {
        e.stopPropagation();
        if (accessible === false) return;
        navigate(`/group/${groupId}/task/${taskId}/comments`);
    }

    async function handleConfirmDelete() {
        if (!confirmDeleteId) return;
        // snapshot - state may change while the request is in flight
        const id = confirmDeleteId;
        setConfirmDeleteId(null);
        setDeletingId(id);
        try {
            await apiDelete(`/api/groups/${groupId}/task/${id}`);
            showToast("Task deleted.", "success");
            if (onDeleted) onDeleted();
        } catch (err) {
            showToast(err?.message || "Failed to delete task");
        } finally {
            setDeletingId(null);
        }
    }

    return (
        <div className="task-rows-wrapper">
            <div className="task-rows">
                {tasks.map((t) => (
                    <div
                        key={t.i}
                        className={`task-row${t.a === false ? " task-row-locked" : ""}${flashId === t.i ? " task-row-denied" : ""}`}
                        style={gridStyle}
                        onClick={() => {
                            if (t.a === false) {
                                flashRow(t.i);
                            } else {
                                navigate(`/group/${groupId}/task/${t.i}`);
                            }
                        }}
                    >
                        {visSet.has(0) && (
                            <span className="task-cell cell-title" title={t.t}>
                                {t.t}
                            </span>
                        )}
                        {visSet.has(1) && (
                            <span className="task-cell cell-creator">
                                {t.cn || "-"}
                            </span>
                        )}
                        {visSet.has(2) && (
                            <span className="task-cell cell-priority">
                                {priorityTag(t.p)}
                            </span>
                        )}
                        {visSet.has(3) && (
                            <span className="task-cell cell-due">
                                {formatDate(t.dd, "-")}
                            </span>
                        )}
                        {visSet.has(4) && (
                            <span className="task-cell cell-access">
                                {t.a ? "✓" : <FiLock size={14} className="lock-icon" title="Not accessible" />}
                            </span>
                        )}
                        {visSet.has(5) && (
                            <span
                                className="task-cell cell-comments"
                                onClick={(e) => goToComments(e, t.i, t.a)}
                                title="Go to comments"
                            >
                                <span
                                    className={`comment-icon${t.nc ? " has-new" : ""}`}
                                    title={
                                        t.nc
                                            ? "New comments"
                                            : `${t.cc ?? 0} comments`
                                    }
                                >
                                    <FiMessageCircle size={16} />
                                    {(t.cc ?? 0) > 0 && (
                                        <span className="comment-count">
                                            {t.cc}
                                        </span>
                                    )}
                                </span>
                            </span>
                        )}
                        {canManageTasks && (
                            <span
                                className="task-cell cell-delete"
                                onClick={(e) => e.stopPropagation()}
                            >
                                <button
                                    className={`task-row-delete-btn${t.dl === false ? " disabled" : ""}`}
                                    disabled={t.dl === false || deletingId === t.i}
                                    title={t.dl === false ? "Cannot delete \u2014 creator still holds a protected role" : "Delete task"}
                                    onClick={(e) => { e.stopPropagation(); if (t.dl !== false) setConfirmDeleteId(t.i); }}
                                >
                                    <FiTrash2 size={14} />
                                </button>
                            </span>
                        )}
                    </div>
                ))}
            </div>

            {confirmDeleteId && (
                <div className="task-delete-overlay" onClick={() => setConfirmDeleteId(null)}>
                    <div className="task-delete-modal" onClick={(e) => e.stopPropagation()}>
                        <h3>Delete task?</h3>
                        <p>This action cannot be undone. The task and all its files and comments will be permanently deleted.</p>
                        <div className="task-delete-modal-actions">
                            <button className="btn-danger" onClick={handleConfirmDelete}>Delete</button>
                            <button className="btn-secondary" onClick={() => setConfirmDeleteId(null)}>Cancel</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
