import { useState } from "react";
import { apiPatch, apiDelete } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { useBlobUrl } from "@context/BlobSasContext";
import { formatRole } from "@assets/js/formatLabel";
import "@styles/popups/Popup.css";
import "@styles/popups/MemberDetailPopup.css";

const ASSIGNABLE_ROLES = ["GUEST", "MEMBER", "REVIEWER", "TASK_MANAGER"];

export default function MemberDetailPopup({
    member,
    groupId,
    isGroupLeader,
    isSelf,
    taskPreviews,
    onClose,
    onRefresh,
    isOnline,
}) {
    const showToast = useToast();
    const blobUrl = useBlobUrl();
    const [newRole, setNewRole] = useState(member.role);
    const [busy, setBusy] = useState(false);
    const [confirmRemove, setConfirmRemove] = useState(false);

    const userName = member.user?.name || member.user?.email || "Unknown";
    const imgSrc = member.user?.imgUrl
        ? blobUrl(member.user.imgUrl)
        : member.user?.defaultImgUrl
            ? blobUrl(member.user.defaultImgUrl)
            : "";

    const createdCount = taskPreviews
        ? taskPreviews.filter((t) => t.cn === member.user?.name).length
        : null;

    const canManage = isGroupLeader && !isSelf;
    const roleChanged = newRole !== member.role;

    async function handleChangeRole() {
        if (!roleChanged) return;
        setBusy(true);
        try {
            await apiPatch(
                `/api/groups/${groupId}/groupMembership/${member.id}/role?role=${newRole}`
            );
            showToast("Role updated", "success");
            if (onRefresh) onRefresh();
            onClose();
        } catch (err) {
            showToast(err?.message || "Failed to change role");
        } finally {
            setBusy(false);
        }
    }

    async function handleRemove() {
        setBusy(true);
        try {
            await apiDelete(`/api/groups/${groupId}/groupMembership/${member.id}`);
            showToast("Member removed", "success");
            if (onRefresh) onRefresh();
            onClose();
        } catch (err) {
            showToast(err?.message || "Failed to remove member");
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card mdp-card" onClick={(e) => e.stopPropagation()}>
                { }
                <div className="mdp-header">
                    <div className="mdp-avatar-wrapper">
                        <span className={`mdp-status-dot${isOnline ? " online" : ""}`} />
                        {imgSrc ? (
                            <img src={imgSrc} alt="" className={`mdp-avatar tier-ring-${member.user?.subscriptionPlan || 'FREE'}`} />
                        ) : (
                            <span className={`mdp-avatar mdp-avatar-placeholder tier-ring-${member.user?.subscriptionPlan || 'FREE'}`} />
                        )}
                    </div>
                    <div className="mdp-header-info">
                        <h2 className="mdp-name">{userName}</h2>
                        <span className="mdp-role-badge">{formatRole(member.role)}</span>
                        {member.user?.sameOrg && (
                            <span className="mdp-org-badge">ORG</span>
                        )}
                    </div>
                </div>

                { }
                {createdCount !== null && (
                    <div className="mdp-stats">
                        <div className="mdp-stat">
                            <span className="mdp-stat-value">{createdCount}</span>
                            <span className="mdp-stat-label">Tasks created</span>
                        </div>
                    </div>
                )}

                { }
                {canManage && (
                    <div className="mdp-section">
                        <label className="mdp-label">Change role</label>
                        <div className="mdp-role-row">
                            <select
                                className="mdp-select"
                                value={newRole}
                                onChange={(e) => setNewRole(e.target.value)}
                                disabled={busy}
                            >
                                {ASSIGNABLE_ROLES.map((r) => (
                                    <option key={r} value={r}>
                                        {formatRole(r)}
                                    </option>
                                ))}
                            </select>
                            <button
                                className="btn-primary mdp-save-btn"
                                disabled={busy || !roleChanged}
                                onClick={handleChangeRole}
                            >
                                {busy ? "Saving…" : "Save"}
                            </button>
                        </div>
                    </div>
                )}

                { }
                {canManage && (
                    <div className="mdp-section mdp-remove-section">
                        {!confirmRemove ? (
                            <button
                                className="btn-danger-outline mdp-remove-btn"
                                onClick={() => setConfirmRemove(true)}
                                disabled={busy}
                            >
                                Remove from group
                            </button>
                        ) : (
                            <div className="mdp-remove-confirm">
                                <span className="mdp-remove-warn">Remove {userName}?</span>
                                <button
                                    className="btn-danger mdp-confirm-yes"
                                    onClick={handleRemove}
                                    disabled={busy}
                                >
                                    {busy ? "Removing…" : "Yes, remove"}
                                </button>
                                <button
                                    className="btn-secondary mdp-confirm-no"
                                    onClick={() => setConfirmRemove(false)}
                                    disabled={busy}
                                >
                                    Cancel
                                </button>
                            </div>
                        )}
                    </div>
                )}

                { }
                <div className="popup-actions">
                    <button className="btn-secondary" onClick={onClose} disabled={busy}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}
