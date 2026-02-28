// Assignee list + picker. Shares participant styles with TaskReviewPanel.
import { FiPlus } from "react-icons/fi";
import userImg from "@assets/js/userImg";

export default function TaskAssigneePanel({
    assignees, eligibleAssignees, showPicker, onTogglePicker,
    canAddParticipants, onAddAssignee, blobUrl,
}) {
    return (
        <div className="task-sidebar-section">
            <h4>
                Assignees
                {canAddParticipants && (
                    <button
                        className="task-sidebar-add"
                        title="Add assignee"
                        onClick={onTogglePicker}
                    >
                        <FiPlus size={12} />
                    </button>
                )}
            </h4>

            {/* picker */}
            {showPicker && canAddParticipants && (
                <div className="task-participant-picker">
                    {eligibleAssignees.length === 0 ? (
                        <span className="task-participant-picker-empty">No eligible members</span>
                    ) : (
                        eligibleAssignees.map((m) => (
                            <div
                                key={m.user?.id}
                                className="task-participant-picker-item"
                                onClick={() => onAddAssignee(m.user?.id)}
                            >
                                <img src={userImg(m.user, blobUrl)} alt="" className="task-participant-img" />
                                <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                <span className="task-participant-role-tag">{m.role.replace("_", " ")}</span>
                            </div>
                        ))
                    )}
                </div>
            )}

            {/* participant list */}
            <div className="task-participant-list">
                {assignees.length === 0 ? (
                    <span className="muted">No assignees</span>
                ) : (
                    assignees.map((a) => (
                        <div key={a.id} className="task-participant-row">
                            <img src={userImg(a.user, blobUrl)} alt="" className="task-participant-img" />
                            <span title={a.user?.email}>{a.user?.name || a.user?.email}</span>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}
