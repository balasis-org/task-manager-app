import { useState, useContext } from "react";
import { FiCopy, FiRefreshCw } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { apiPost } from "@assets/js/apiClient.js";
import { useToast } from "@context/ToastContext";
import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/layout/SidebarProfile.css";

// mini profile card in the sidebar: avatar with tier ring, name, invite code
// with copy + refresh buttons. tier-ring-{plan} CSS class draws a colored
// border ring matching the subscription tier.
export default function SidebarProfile({ user }) {
    const blobUrl = useBlobUrl();
    const { setUser } = useContext(AuthContext);
    const showToast = useToast();
    const [refreshing, setRefreshing] = useState(false);

    const rawImgPath = user?.imgUrl || user?.defaultImgUrl || null;
    const profileImg = rawImgPath ? blobUrl(rawImgPath) : null;

    function handleCopy() {
        if (user?.inviteCode) {
            navigator.clipboard.writeText(user.inviteCode);
            showToast("Invite code copied!", "success");
        }
    }

    async function handleRefresh() {
        setRefreshing(true);
        try {
            const updated = await apiPost("/api/users/me/refresh-invite-code");
            setUser((prev) => (prev ? { ...prev, inviteCode: updated.inviteCode } : prev));
            showToast("Invite code refreshed!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to refresh code");
        } finally {
            setRefreshing(false);
        }
    }

    return (
        <div className="sidebar-profile">
            {profileImg ? (
                <img
                    src={profileImg}
                    alt="profile"
                    className={`sidebar-profile-img tier-ring-${user?.subscriptionPlan || 'FREE'}`}
                />
            ) : (
                <div className={`sidebar-profile-img tier-ring-${user?.subscriptionPlan || 'FREE'}`} />
            )}
            {user?.name && (
                <span className="sidebar-profile-name">{user.name}</span>
            )}
            {user?.inviteCode && (
                <div className="sidebar-profile-code-row">
                    <span className="sidebar-profile-code">{user.inviteCode}</span>
                    <button className="sidebar-code-btn" onClick={handleCopy} title="Copy code">
                        <FiCopy size={11} />
                    </button>
                    <button className="sidebar-code-btn" onClick={handleRefresh} title="Refresh code" disabled={refreshing}>
                        <FiRefreshCw size={11} className={refreshing ? "spin" : ""} />
                    </button>
                </div>
            )}
        </div>
    );
}
