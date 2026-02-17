import { useState } from "react";
import { apiGet, apiPost } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { LIMITS } from "@assets/js/inputValidation";
import "@styles/popups/Popup.css";
import blobBase from "@blobBase";

const ROLES = ["MEMBER", "GUEST", "REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];

export default function InviteToGroupPopup({ groupId, onClose }) {
    const showToast = useToast();
    const [search, setSearch] = useState("");
    const [results, setResults] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [role, setRole] = useState("MEMBER");
    const [comment, setComment] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const handleSearch = async (q) => {
        setSearch(q);
        if (q.length < 2) {
            setResults([]);
            return;
        }
        try {
            const data = await apiGet(
                `/api/users/search?q=${encodeURIComponent(q)}&page=0&size=40`
            );
            setResults(data?.content ?? []);
        } catch {
            setResults([]);
        }
    };

    const handleInvite = async (e) => {
        e.preventDefault();
        if (!selectedUser) {
            setError("Select a user to invite.");
            return;
        }
        setBusy(true);
        setError("");
        try {
            await apiPost(`/api/groups/${groupId}/invite`, {
                userId: selectedUser.id,
                userToBeInvitedRole: role,
                comment: comment.trim(),
            });
            onClose();
            showToast("Invitation sent!", "success");
        } catch {
            setError("Failed to send invitation.");
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
                        Search
                        <input
                            type="text"
                            value={search}
                            onChange={(e) => handleSearch(e.target.value)}
                            placeholder="Type to search members…"
                        />
                    </label>

                    {results.length > 0 && (
                        <div className="popup-search-results">
                            {results.map((u) => (
                                <div
                                    key={u.id}
                                    className={`popup-search-item${selectedUser?.id === u.id ? " selected" : ""}`}
                                    onClick={() => setSelectedUser(u)}
                                >
                                    <img
                                        src={u.imgUrl ? blobBase + u.imgUrl : (u.defaultImgUrl ? blobBase + u.defaultImgUrl : "")}
                                        alt=""
                                        className="popup-search-img"
                                    />
                                    <span>{u.name || u.email}</span>
                                </div>
                            ))}
                        </div>
                    )}

                    {selectedUser && (
                        <div className="popup-selected-user">
                            Selected: <strong>{selectedUser.name || selectedUser.email}</strong>
                        </div>
                    )}

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
                        <button type="submit" className="btn-primary" disabled={busy}>
                            {busy ? "Sending…" : "Confirm"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
