// Reviewer sidebar: participants, last review result, and the review form.
// The review form only shows for members with canReview permission.
import { FiPlus, FiX, FiMail } from "react-icons/fi";
import { LIMITS } from "@assets/js/inputValidation";
import userImg from "@assets/js/userImg";
import "@styles/task/TaskReviewPanel.css";

const DECISION_OPTIONS = ["APPROVE", "REJECT"];

export default function TaskReviewPanel({
    reviewers, eligibleReviewers, showPicker, onTogglePicker,
    canAddParticipants, onAddReviewer, onRemoveParticipant, onNotify, onBulkNotify, blobUrl, task,
    canReview, reviewComment, onReviewCommentChange,
    reviewDecision, onReviewDecisionChange, submittingReview, onReview,
    presenceUserIds,
}) {
    const onlineSet = new Set(presenceUserIds || []);
    return (
        <div className="task-sidebar-section">
            <h4>
                Reviewer
                {onBulkNotify && reviewers.length > 0 && (
                    <button
                        className="task-sidebar-add task-bulk-notify"
                        title="Email all reviewers"
                        onClick={() => onBulkNotify("reviewers")}
                    >
                        <FiMail size={13} />
                    </button>
                )}
                {canAddParticipants && (
                    <button
                        className="task-sidebar-add"
                        title="Add reviewer"
                        onClick={onTogglePicker}
                    >
                        <FiPlus size={12} />
                    </button>
                )}
            </h4>

            {/* picker */}
            {showPicker && canAddParticipants && (
                <div className="task-participant-picker">
                    {eligibleReviewers.length === 0 ? (
                        <span className="task-participant-picker-empty">No eligible members</span>
                    ) : (
                        eligibleReviewers.map((m) => (
                            <div
                                key={m.user?.id}
                                className="task-participant-picker-item"
                                onClick={() => onAddReviewer(m.user?.id)}
                            >
                                <img src={userImg(m.user, blobUrl)} alt="" className="task-participant-img" />
                                <span className={`task-presence-dot picker-dot${onlineSet.has(m.user?.id) ? " online" : ""}`} />
                                <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                <span className="task-participant-role-tag">{m.role.replace("_", " ")}</span>
                            </div>
                        ))
                    )}
                </div>
            )}

            {/* participant list */}
            <div className="task-participant-list">
                {reviewers.length === 0 ? (
                    <span className="muted">No reviewer</span>
                ) : (
                    reviewers.map((r) => {
                        const reachable = r.user?.email && r.user?.allowEmailNotification;
                        return (
                        <div key={r.id} className="task-participant-row">
                            <img src={userImg(r.user, blobUrl)} alt="" className="task-participant-img" />
                            <span className={`task-presence-dot${onlineSet.has(r.user?.id) ? " online" : ""}`} />
                            <span title={r.user?.email}>{r.user?.name || r.user?.email}</span>
                            {onNotify && (
                                <button
                                    className={`task-participant-email${reachable ? "" : " unreachable"}`}
                                    title={reachable ? "Send email notification" : "User has no email or disabled notifications"}
                                    onClick={() => reachable && onNotify(r.user?.id)}
                                    disabled={!reachable}
                                >
                                    <FiMail size={15} />
                                </button>
                            )}
                            {onRemoveParticipant && (
                                <button
                                    className="task-participant-remove"
                                    title="Remove reviewer"
                                    onClick={() => onRemoveParticipant(r.id)}
                                >
                                    <FiX size={14} />
                                </button>
                            )}
                        </div>
                        );
                    })
                )}
            </div>

            {/* review info */}
            <div className="task-review-info">
                <div className="task-info-row">
                    <span className="task-info-label">Latest review by:</span>
                    <span>{task.reviewedBy?.name || task.reviewedBy?.email || "-"}</span>
                </div>
                <div className="task-info-row">
                    <span className="task-info-label">Latest review comment:</span>
                    <span>{task.reviewComment || "-"}</span>
                </div>
                <div className="task-info-row">
                    <span className="task-info-label">Decision:</span>
                    <span
                        className={`task-decision ${
                            task.reviewersDecision === "APPROVE"
                                ? "approved"
                                : task.reviewersDecision === "REJECT"
                                  ? "rejected"
                                  : ""
                        }`}
                    >
                        {task.reviewersDecision || "-"}
                    </span>
                </div>
            </div>

            {/* review form */}
            {canReview && (
                <div className="task-review-form">
                    <label>Reviewer Comment</label>
                    <div className="task-review-textarea-wrapper">
                        <textarea
                            className="task-review-textarea"
                            value={reviewComment}
                            onChange={(e) =>
                                onReviewCommentChange(e.target.value.slice(0, LIMITS.TASK_REVIEW_COMMENT))
                            }
                            placeholder="Your review comment…"
                            rows={3}
                            maxLength={LIMITS.TASK_REVIEW_COMMENT}
                        />
                        <span className="task-review-counter">
                            {reviewComment.length}/{LIMITS.TASK_REVIEW_COMMENT}
                        </span>
                    </div>
                    <div className="task-review-actions">
                        <label>Decision:</label>
                        <select
                            value={reviewDecision}
                            onChange={(e) => onReviewDecisionChange(e.target.value)}
                            className="task-review-decision"
                        >
                            {DECISION_OPTIONS.map((d) => (
                                <option key={d} value={d}>{d}</option>
                            ))}
                        </select>
                        <button
                            className="btn-primary btn-sm"
                            disabled={submittingReview}
                            onClick={onReview}
                        >
                            Confirm
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
