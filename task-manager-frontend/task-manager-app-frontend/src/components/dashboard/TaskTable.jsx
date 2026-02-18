import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { FiMessageCircle, FiLock } from "react-icons/fi";
import "@styles/dashboard/TaskTable.css";

function formatDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function priorityTag(p) {
    if (p == null) return "—";
    let label, cls;
    if (p <= 4)      { label = "Low";    cls = "priority-low"; }
    else if (p <= 7) { label = "Medium"; cls = "priority-med"; }
    else             { label = "High";   cls = "priority-high"; }
    return <span className={`priority-tag ${cls}`}>{label} ({p})</span>;
}

export default function TaskTable({ tasks, groupId, colWidths, visCols }) {
    const navigate = useNavigate();
    const [flashId, setFlashId] = useState(null);

    const flashRow = useCallback((id) => {
        setFlashId(id);
        setTimeout(() => setFlashId(null), 500);
    }, []);

    if (!tasks || tasks.length === 0) {
        return <div className="task-table-empty">No tasks</div>;
    }

    // Build grid template for visible columns only
    const gridStyle = colWidths && visCols
        ? { gridTemplateColumns: visCols.map((ci) => (ci === 0 ? "minmax(0,1fr)" : colWidths[ci] + "px")).join(" ") }
        : undefined;

    // visible column indices as a Set for fast lookup
    const visSet = new Set(visCols || [0, 1, 2, 3, 4, 5]);

    function goToComments(e, taskId, accessible) {
        e.stopPropagation();
        if (accessible === false) return;
        navigate(`/group/${groupId}/task/${taskId}/comments`);
    }

    return (
        <div className="task-rows">
            {tasks.map((t) => (
                <div
                    key={t.id}
                    className={`task-row${t.accessible === false ? " task-row-locked" : ""}${flashId === t.id ? " task-row-denied" : ""}`}
                    style={gridStyle}
                    onClick={() => {
                        if (t.accessible === false) {
                            flashRow(t.id);
                        } else {
                            navigate(`/group/${groupId}/task/${t.id}`);
                        }
                    }}
                >
                    {visSet.has(0) && (
                        <span className="task-cell cell-title" title={t.title}>
                            {t.title}
                        </span>
                    )}
                    {visSet.has(1) && (
                        <span className="task-cell cell-creator">
                            {t.creatorName || "—"}
                        </span>
                    )}
                    {visSet.has(2) && (
                        <span className="task-cell cell-priority">
                            {priorityTag(t.priority)}
                        </span>
                    )}
                    {visSet.has(3) && (
                        <span className="task-cell cell-due">
                            {formatDate(t.dueDate)}
                        </span>
                    )}
                    {visSet.has(4) && (
                        <span className="task-cell cell-access">
                            {t.accessible ? "✓" : <FiLock size={14} className="lock-icon" title="Not accessible" />}
                        </span>
                    )}
                    {visSet.has(5) && (
                        <span
                            className="task-cell cell-comments"
                            onClick={(e) => goToComments(e, t.id, t.accessible)}
                            title="Go to comments"
                        >
                            <span
                                className={`comment-icon${t.newCommentsToBeRead ? " has-new" : ""}`}
                                title={
                                    t.newCommentsToBeRead
                                        ? "New comments"
                                        : `${t.commentCount ?? 0} comments`
                                }
                            >
                                <FiMessageCircle size={16} />
                                {(t.commentCount ?? 0) > 0 && (
                                    <span className="comment-count">
                                        {t.commentCount}
                                    </span>
                                )}
                            </span>
                        </span>
                    )}
                </div>
            ))}
        </div>
    );
}
