import { useState } from "react";
import { apiPatch, apiDelete } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/popups/Popup.css";
import "@styles/popups/MemberDetailPopup.css";

const ASSIGNABLE_ROLES = ["GUEST", "MEMBER", "REVIEWER", "TASK_MANAGER"];

export default function MemberDetailPopup({
    member,          // the membership object { id, role, user: { id, name, email, imgUrl, defaultImgUrl, sameOrg } }
    groupId,
    isGroupLeader,   // current user is GL
    isSelf,          // current user === this member
    taskPreviews,    // groupDetail.tp — may be null/undefined
    onClose,
    onRefresh,       // called after role change or removal
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

    // Derive task counts from task previews (only creatorName (cn) is available)
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
                {/* Header with image + name */}
                <div className="mdp-header">
                    {imgSrc ? (
                        <img src={imgSrc} alt="" className="mdp-avatar" />
                    ) : (
                        <span className="mdp-avatar mdp-avatar-placeholder" />
                    )}
                    <div className="mdp-header-info">
                        <h2 className="mdp-name">{userName}</h2>
                        <span className="mdp-role-badge">{member.role.replace(/_/g, " ")}</span>
                        {member.user?.sameOrg && (
                            <span className="mdp-org-badge">ORG</span>
                        )}
                    </div>
                </div>

                {/* Task stats (if data available) */}
                {createdCount !== null && (
                    <div className="mdp-stats">
                        <div className="mdp-stat">
                            <span className="mdp-stat-value">{createdCount}</span>
                            <span className="mdp-stat-label">Tasks created</span>
                        </div>
                    </div>
                )}

                {/* Role change (GL only, not for self) */}
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
                                        {r.replace(/_/g, " ")}
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
                        <span className="mdp-hint">
                            Leadership can only be transferred through Group Settings → Transfer Leadership.
                        </span>
                    </div>
                )}

                {/* Remove member (GL only, not self) */}
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

                {/* Close */}
                <div className="popup-actions">
                    <button className="btn-secondary" onClick={onClose} disabled={busy}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}
