import { useNavigate } from "react-router-dom";
import { FiMessageCircle } from "react-icons/fi";
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

export default function TaskTable({ tasks, groupId, colWidths }) {
    const navigate = useNavigate();

    if (!tasks || tasks.length === 0) {
        return <div className="task-table-empty">No tasks</div>;
    }

    const gridStyle = colWidths
        ? { gridTemplateColumns: `1fr ${colWidths.slice(1).map((w) => w + "px").join(" ")}` }
        : undefined;

    function goToComments(e, taskId) {
        e.stopPropagation();
        navigate(`/group/${groupId}/task/${taskId}/comments`);
    }

    return (
        <div className="task-rows">
            {tasks.map((t) => (
                <div
                    key={t.id}
                    className="task-row"
                    style={gridStyle}
                    onClick={() =>
                        navigate(`/group/${groupId}/task/${t.id}`)
                    }
                >
                    <span className="task-cell cell-title" title={t.title}>
                        {t.title}
                    </span>
                    <span className="task-cell cell-creator">
                        {t.creatorName || "—"}
                    </span>
                    <span className="task-cell cell-priority">
                        {priorityTag(t.priority)}
                    </span>
                    <span className="task-cell cell-due">
                        {formatDate(t.dueDate)}
                    </span>
                    <span className="task-cell cell-access">
                        {t.accessible ? "✓" : "—"}
                    </span>
                    <span
                        className="task-cell cell-comments"
                        onClick={(e) => goToComments(e, t.id)}
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
                </div>
            ))}
        </div>
    );
}
