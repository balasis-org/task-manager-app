// Breadcrumb bar + action buttons (edit, delete, chat link).
// `creator` can be null if the user was removed from the system.
import { Link } from "react-router-dom";
import {
    FiArrowLeft, FiMessageCircle, FiEdit2,
    FiTrash2, FiChevronRight,
} from "react-icons/fi";
import "@styles/task/TaskBreadcrumb.css";

export default function TaskBreadcrumb({
    groupId, taskId, groupName, creator, lastEditBy,
    canEdit, canDelete, editing, onEdit, onDelete,
    presenceUserIds,
}) {
    const onlineSet = new Set(presenceUserIds || []);
    return (
        <div className="task-breadcrumb">
            <Link to="/dashboard" className="task-breadcrumb-back" title="Back to group">
                <FiArrowLeft size={14} />
                <span>Back to group</span>
            </Link>

            <span className="task-breadcrumb-trail">
                <Link to="/dashboard" className="breadcrumb-link" title={groupName}>
                    {groupName}
                </Link>
                <FiChevronRight size={12} className="breadcrumb-sep" />
                <span className="breadcrumb-current">Task</span>
            </span>

            <div className="task-breadcrumb-right">
                <span className="task-meta-byline">
                    By: {creator?.user?.name || creator?.user?.email || "-"}
                    {creator?.user?.id && (
                        <span className={`task-presence-dot${onlineSet.has(creator.user.id) ? " online" : ""}`} />
                    )}
                </span>

                {canEdit && !editing && (
                    <button className="task-edit-btn" onClick={onEdit} title="Edit task">
                        <FiEdit2 size={14} /> Edit
                    </button>
                )}

                {canDelete && (
                    <button className="task-delete-btn" onClick={onDelete} title="Delete task">
                        <FiTrash2 size={14} /> Delete
                    </button>
                )}

                <span className="task-meta-group" title={groupName}>
                    Group: {groupName}
                </span>

                {lastEditBy && (
                    <span className="task-meta-lastedit">
                        LastEditBy: {lastEditBy.name || lastEditBy.email}
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
    );
}
