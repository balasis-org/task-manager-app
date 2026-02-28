// Reviewer sidebar: participants, last review result, and the review form.
// The review form only shows for members with canReview permission.
import { FiPlus } from "react-icons/fi";
import { LIMITS } from "@assets/js/inputValidation";
import userImg from "@assets/js/userImg";
import "@styles/task/TaskReviewPanel.css";

const DECISION_OPTIONS = ["APPROVE", "REJECT"];

export default function TaskReviewPanel({
    reviewers, eligibleReviewers, showPicker, onTogglePicker,
    canAddParticipants, onAddReviewer, blobUrl, task,
    canReview, reviewComment, onReviewCommentChange,
    reviewDecision, onReviewDecisionChange, submittingReview, onReview,
}) {
    return (
        <div className="task-sidebar-section">
            <h4>
                Reviewer
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
                    reviewers.map((r) => (
                        <div key={r.id} className="task-participant-row">
                            <img src={userImg(r.user, blobUrl)} alt="" className="task-participant-img" />
                            <span title={r.user?.email}>{r.user?.name || r.user?.email}</span>
                        </div>
                    ))
                )}
            </div>

            {/* review info */}
            <div className="task-review-info">
                <div className="task-info-row">
                    <span className="task-info-label">Latest review by:</span>
                    <span>{task.reviewedBy?.name || task.reviewedBy?.email || "—"}</span>
                </div>
                <div className="task-info-row">
                    <span className="task-info-label">Latest review comment:</span>
                    <span>{task.reviewComment || "—"}</span>
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
                        {task.reviewersDecision || "—"}
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
