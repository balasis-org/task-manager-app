import { useState } from "react";
import { apiPost } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { LIMITS } from "@assets/js/inputValidation";
import "@styles/popups/Popup.css";

const ROLES = [
    { value: "MEMBER",        label: "Member",        hint: "Can be assigned to tasks and upload files" },
    { value: "GUEST",         label: "Guest",         hint: "View-only access, cannot be assigned" },
    { value: "REVIEWER",      label: "Reviewer",      hint: "Can review and approve or reject work" },
    { value: "TASK_MANAGER",  label: "Task Manager",  hint: "Can create, edit, and manage tasks" },
    { value: "GROUP_LEADER",  label: "Group Leader",  hint: "Full control including settings and invitations" },
];

// invite-by-code form: the inviter enters the target user's invite code
// (not an email address). the backend resolves the code to a user.
// email notification toggle only visible on ORGANIZER+ plans.
export default function InviteToGroupPopup({ groupId, groupDetail, onClose }) {
    const showToast = useToast();
    const [inviteCode, setInviteCode] = useState("");
    const [role, setRole] = useState("MEMBER");
    const [comment, setComment] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const emailAvailable = groupDetail?.op !== "FREE" && groupDetail?.op !== "STUDENT";
    const [sendEmail, setSendEmail] = useState(false);

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
                sendEmail: emailAvailable && sendEmail,
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
                                <option key={r.value} value={r.value}>
                                    {r.label}
                                </option>
                            ))}
                        </select>
                        <span className="popup-hint">
                            {ROLES.find((r) => r.value === role)?.hint}
                        </span>
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

                    <label className={`invite-email-check${emailAvailable ? "" : " disabled"}`}>
                        <input
                            type="checkbox"
                            checked={emailAvailable && sendEmail}
                            onChange={(e) => setSendEmail(e.target.checked)}
                            disabled={!emailAvailable}
                        />
                        Send email notification
                        {emailAvailable && (
                            <span className="popup-hint" style={{ marginLeft: 4 }}>
                                ({groupDetail?.ue ?? 0}/{groupDetail?.eq ?? 0})
                            </span>
                        )}
                        {!emailAvailable && (
                            <span className="popup-hint" style={{ marginLeft: 4 }}>
                                (requires Organizer or Team plan)
                            </span>
                        )}
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
