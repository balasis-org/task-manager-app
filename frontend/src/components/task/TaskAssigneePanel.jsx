// Assignee list + picker. Shares participant styles with TaskReviewPanel.
import { FiPlus, FiX, FiMail } from "react-icons/fi";
import userImg from "@assets/js/userImg";
import { formatRole } from "@assets/js/formatLabel";

export default function TaskAssigneePanel({
    assignees, eligibleAssignees, showPicker, onTogglePicker,
    canAddParticipants, onAddAssignee, onRemoveParticipant, onNotify, onBulkNotify, blobUrl,
    presenceUserIds,
}) {
    const onlineSet = new Set(presenceUserIds || []);
    return (
        <div className="task-sidebar-section">
            <h4>
                Assignees
                {onBulkNotify && assignees.length > 0 && (
                    <button
                        className="task-sidebar-add task-bulk-notify"
                        title="Email all assignees"
                        onClick={() => onBulkNotify("assignees")}
                    >
                        <FiMail size={13} />
                    </button>
                )}
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
                                <span className={`task-presence-dot picker-dot${onlineSet.has(m.user?.id) ? " online" : ""}`} />
                                <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                <span className="task-participant-role-tag">{formatRole(m.role)}</span>
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
                    assignees.map((a) => {
                        const reachable = a.user?.email && a.user?.allowEmailNotification;
                        return (
                        <div key={a.id} className="task-participant-row">
                            <img src={userImg(a.user, blobUrl)} alt="" className="task-participant-img" />
                            <span className={`task-presence-dot${onlineSet.has(a.user?.id) ? " online" : ""}`} />
                            <span title={a.user?.email}>{a.user?.name || a.user?.email}</span>
                            {onNotify && (
                                <button
                                    className={`task-participant-email${reachable ? "" : " unreachable"}`}
                                    title={reachable ? "Send email notification" : "User has no email or disabled notifications"}
                                    onClick={() => reachable && onNotify(a.user?.id)}
                                    disabled={!reachable}
                                >
                                    <FiMail size={15} />
                                </button>
                            )}
                            {onRemoveParticipant && (
                                <button
                                    className="task-participant-remove"
                                    title="Remove assignee"
                                    onClick={() => onRemoveParticipant(a.id)}
                                >
                                    <FiX size={14} />
                                </button>
                            )}
                        </div>
                        );
                    })
                )}
            </div>
        </div>
    );
}
