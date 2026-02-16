import { useNavigate } from "react-router-dom";
import { FiMessageCircle } from "react-icons/fi";
import "@styles/dashboard/TaskTable.css";

function formatDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

export default function TaskTable({ tasks, groupId }) {
    const navigate = useNavigate();

    if (!tasks || tasks.length === 0) {
        return <div className="task-table-empty">No tasks</div>;
    }

    return (
        <div className="task-rows">
            {tasks.map((t) => (
                <div
                    key={t.id}
                    className="task-row"
                    onClick={() =>
                        navigate(`/task?groupId=${groupId}&taskId=${t.id}`)
                    }
                >
                    <span className="task-cell cell-title" title={t.title}>
                        {t.title}
                    </span>
                    <span className="task-cell cell-creator">
                        {t.creatorName || "—"}
                    </span>
                    <span className="task-cell cell-priority">
                        {t.priority || "—"}
                    </span>
                    <span className="task-cell cell-due">
                        {formatDate(t.dueDate)}
                    </span>
                    <span className="task-cell cell-access">
                        {t.accessible ? "✓" : "—"}
                    </span>
                    <span className="task-cell cell-comments">
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
