import { useState, useContext } from "react";
import { FiCopy, FiRefreshCw } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { useToast } from "@context/ToastContext";
import { apiPost } from "@assets/js/apiClient.js";
import "@styles/settings/SettingsInviteCode.css";

export default function SettingsInviteCode() {
    const { user, setUser } = useContext(AuthContext);
    const showToast = useToast();
    const [refreshingCode, setRefreshingCode] = useState(false);

    async function handleRefreshCode() {
        setRefreshingCode(true);
        try {
            const updated = await apiPost("/api/users/me/refresh-invite-code");
            setUser((prev) => (prev ? { ...prev, inviteCode: updated.inviteCode } : prev));
            showToast("Invite code refreshed!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to refresh invite code");
        } finally {
            setRefreshingCode(false);
        }
    }

    function handleCopyCode() {
        if (user?.inviteCode) {
            navigator.clipboard.writeText(user.inviteCode);
            showToast("Invite code copied!", "success");
        }
    }

    return (
        <div className="settings-field">
            <span className="settings-label">Invite code</span>
            <div className="settings-invite-code-row">
                <span className="settings-invite-code">{user?.inviteCode || "-"}</span>
                <button
                    type="button"
                    className="settings-code-btn"
                    onClick={handleCopyCode}
                    title="Copy code"
                    disabled={!user?.inviteCode}
                >
                    <FiCopy size={14} />
                </button>
                <button
                    type="button"
                    className="settings-code-btn"
                    onClick={handleRefreshCode}
                    title="Generate a new code"
                    disabled={refreshingCode}
                >
                    <FiRefreshCw size={14} className={refreshingCode ? "spin" : ""} />
                </button>
            </div>
            <span className="settings-hint">
                Share this code with others so they can invite you to a group.
                Refresh it to invalidate the old one.
            </span>
        </div>
    );
}
