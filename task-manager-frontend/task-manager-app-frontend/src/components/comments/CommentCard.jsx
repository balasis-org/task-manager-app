// Single comment bubble. Handles the edge case where the creator
// was deleted - falls back to creatorNameSnapshot.
import userImg from "@assets/js/userImg";
import "@styles/comments/CommentCard.css";

export default function CommentCard({
    comment: c, isEditing, editText, onEditTextChange,
    participantRoleMap, canEdit, canDelete,
    onStartEdit, onCancelEdit, onSaveEdit, onRequestDelete,
    blobUrl, maxLen, formatDateTime,
    presenceUserIds,
}) {
    const isDeletedCreator = !c.creator;
    const taskRole = !isDeletedCreator ? participantRoleMap[c.creator?.id] : null;
    const onlineSet = new Set(presenceUserIds || []);
    const isOnline = !isDeletedCreator && onlineSet.has(c.creator?.id);

    return (
        <div className="comment-card">
            <div className="comment-avatar-wrap">
                {isDeletedCreator ? (
                    <div className="comment-avatar comment-avatar-deleted" title="Deleted user" />
                ) : (
                    <img
                        src={userImg(c.creator, blobUrl)}
                        alt=""
                        className="comment-avatar"
                    />
                )}
                {!isDeletedCreator && (
                    <span className={`comment-presence-dot${isOnline ? " online" : ""}`} />
                )}
            </div>
            <div className="comment-body">
                <div className="comment-header">
                    <span className="comment-author">
                        {isDeletedCreator
                            ? <>{c.creatorNameSnapshot || "Unknown"} <span className="comment-deleted-tag">(Deleted User)</span></>
                            : <>{c.creator?.name || c.creator?.email || "Unknown"}
                                {taskRole && (
                                    <span className="comment-role-tag">
                                        ({taskRole.replace("_", " ")})
                                    </span>
                                )}
                              </>
                        }
                    </span>
                    <div className="comment-actions">
                        {!isEditing && canEdit && !isDeletedCreator && (
                            <button
                                className="comment-action-btn"
                                title="Edit"
                                onClick={() => onStartEdit(c)}
                            >
                                Edit
                            </button>
                        )}
                        {!isEditing && canDelete && (
                            <button
                                className="comment-action-btn danger"
                                title="Delete"
                                onClick={() => onRequestDelete(c.id)}
                            >
                                Delete
                            </button>
                        )}
                    </div>
                </div>

                {isEditing ? (
                    <div className="comment-edit-area">
                        <textarea
                            className="comment-edit-textarea"
                            value={editText}
                            onChange={(e) =>
                                onEditTextChange(e.target.value.slice(0, maxLen))
                            }
                            rows={3}
                            maxLength={maxLen}
                        />
                        <div className="comment-edit-footer">
                            <span className="comment-counter">
                                {editText.length}/{maxLen}
                            </span>
                            <div className="comment-edit-btns">
                                <button
                                    className="btn-cancel-edit"
                                    onClick={onCancelEdit}
                                >
                                    Cancel edit
                                </button>
                                <button
                                    className="btn-save-edit"
                                    onClick={() => onSaveEdit(c.id)}
                                >
                                    Save
                                </button>
                            </div>
                        </div>
                    </div>
                ) : (
                    <p className="comment-text">{c.comment}</p>
                )}

                <span className="comment-date">
                    {formatDateTime(c.createdAt)}
                </span>
            </div>
        </div>
    );
}
