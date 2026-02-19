import { useState } from "react";
import { apiPost } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { LIMITS } from "@assets/js/inputValidation";
import "@styles/popups/Popup.css";

const ROLES = ["MEMBER", "GUEST", "REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];

export default function InviteToGroupPopup({ groupId, onClose }) {
    const showToast = useToast();
    const [inviteCode, setInviteCode] = useState("");
    const [role, setRole] = useState("MEMBER");
    const [comment, setComment] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const handleInvite = async (e) => {
        e.preventDefault();
        const code = inviteCode.trim();
        if (!code) {
            setError("Enter an invite code.");
            return;
        }
        setBusy(true);
        setError("");
        try {
            await apiPost(`/api/groups/${groupId}/invite`, {
                inviteCode: code,
                userToBeInvitedRole: role,
                comment: comment.trim(),
            });
            onClose();
            showToast("If the code is valid, the invitation has been sent.", "success");
        } catch (err) {
            setError(err?.message || "Something went wrong.");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card" onClick={(e) => e.stopPropagation()}>
                <h2>Invite to group</h2>

                {error && <div className="popup-error">{error}</div>}

                <form onSubmit={handleInvite} className="popup-form">
                    <label>
                        Invite code
                        <input
                            type="text"
                            value={inviteCode}
                            onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                            placeholder="e.g. A3B7KX9P"
                            maxLength={8}
                            autoFocus
                            style={{ fontFamily: "'Consolas','Courier New',monospace", letterSpacing: "0.15em" }}
                        />
                        <span className="popup-hint">
                            Ask the person for their invite code (visible on their profile).
                        </span>
                    </label>

                    <label>
                        Role
                        <select value={role} onChange={(e) => setRole(e.target.value)}>
                            {ROLES.map((r) => (
                                <option key={r} value={r}>
                                    {r}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label>
                        Comment (optional)
                        <textarea
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            rows={2}
                            maxLength={LIMITS.INVITE_COMMENT}
                        />
                        <span className="char-count">{comment.length}/{LIMITS.INVITE_COMMENT}</span>
                    </label>

                    <div className="popup-actions">
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={onClose}
                            disabled={busy}
                        >
                            Cancel
                        </button>
                        <button type="submit" className="btn-primary" disabled={busy || !inviteCode.trim()}>
                            {busy ? "Sending…" : "Send invite"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
