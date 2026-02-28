import { FiSmile, FiX } from "react-icons/fi";
import "@styles/comments/CommentComposer.css";

// keep this list short — the picker is tiny on mobile
const EMOJIS = ["😀","😂","😍","👍","👎","🎉","🔥","❤️","😢","😮","🤔","😎","💯","✅","❌","⚡"];

export default function CommentComposer({
    open, newComment, onNewCommentChange, onSubmit, submitting,
    showEmojis, onToggleEmojis, onInsertEmoji, onClose,
    textareaRef, maxLen,
}) {
    return (
        <div className={`comments-composer${open ? " open" : ""}`}>
            <div className="composer-bar">
                <span className="composer-label">Make a comment…</span>

                { }
                <div className="composer-emoji-wrapper">
                    <button
                        className="composer-emoji-btn"
                        onClick={onToggleEmojis}
                        title="Emojis"
                    >
                        <FiSmile size={16} /> emojis here.
                    </button>
                    {showEmojis && (
                        <div className="composer-emoji-picker">
                            {EMOJIS.map((em) => (
                                <span
                                    key={em}
                                    className="composer-emoji-item"
                                    onClick={() => onInsertEmoji(em)}
                                >
                                    {em}
                                </span>
                            ))}
                        </div>
                    )}
                </div>

                <span className="composer-hint">pasteable on click</span>

                <button
                    className="composer-close-btn"
                    onClick={onClose}
                    title="Close"
                >
                    <FiX size={14} />
                </button>
            </div>

            <div className="composer-input-row">
                <textarea
                    ref={textareaRef}
                    className="composer-textarea"
                    value={newComment}
                    onChange={(e) => onNewCommentChange(e.target.value.slice(0, maxLen))}
                    placeholder="Write your comment…"
                    rows={2}
                    maxLength={maxLen}
                />
                <span className="composer-counter">
                    {newComment.length}/{maxLen}
                </span>
            </div>

            <div className="composer-submit-row">
                <button
                    className="btn-primary btn-sm"
                    disabled={submitting || !newComment.trim()}
                    onClick={onSubmit}
                >
                    submit
                </button>
            </div>
        </div>
    );
}
