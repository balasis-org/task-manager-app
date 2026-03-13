import "@styles/comments/CommentDeleteModal.css";

export default function CommentDeleteModal({ onConfirm, onCancel }) {
    return (
        <div className="comments-overlay" onClick={onCancel}>
            <div
                className="comments-confirm-popup"
                onClick={(e) => e.stopPropagation()}
            >
                <p>Are you sure you want to delete this comment?</p>
                <div className="confirm-actions">
                    <button
                        className="btn-secondary"
                        onClick={onCancel}
                    >
                        Cancel
                    </button>
                    <button className="btn-danger" onClick={onConfirm}>
                        Delete
                    </button>
                </div>
            </div>
        </div>
    );
}
