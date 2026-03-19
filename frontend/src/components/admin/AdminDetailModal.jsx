// Keeping this as one component with if/else per type rather than splitting
// into UserDetail, GroupDetail, etc. - the shared overlay/header/actions don't
// justify four nearly-identical wrapper files.
import { useState } from "react";
import { FiEye, FiRefreshCw, FiX, FiTrash2, FiDownload } from "react-icons/fi";
import { apiPatch, apiPost } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { formatFileSize } from "@assets/js/fileUtils";
import "@styles/admin/AdminDetailModal.css";

const PLANS = ["FREE", "STUDENT", "ORGANIZER", "TEAM", "TEAMS_PRO"];

// ThresholdBar — reused for storage budget, download budget, email quota.
// turns red (danger class) at 90%+ usage.
function ThresholdBar({ label, used, total, format }) {
    if (!total || total <= 0) return null;
    const pct = Math.min(100, (used / total) * 100);
    const fmt = format || ((v) => v);
    return (
        <div className="admin-threshold-row">
            <label>{label}</label>
            <span className="admin-threshold-text">{fmt(used)} / {fmt(total)}</span>
            <div className="admin-threshold-track">
                <div className={`admin-threshold-fill${pct > 90 ? " danger" : ""}`} style={{ width: `${pct}%` }} />
            </div>
        </div>
    );
}

// detail overlay for admin CRUD — one component handles users, groups,
// tasks, and comments with if/else branches per type. shared overlay and
// action layout makes this simpler than four separate detail components.
export default function AdminDetailModal({
    detailItem, detailLoading, onClose, onRefresh,
    onRequestDelete, blobUrl, onDownload, downloadingId, formatDate,
}) {
    const showToast = useToast();
    const [editPlan, setEditPlan] = useState(null);
    const [confirmPlan, setConfirmPlan] = useState(false);
    const [confirmReset, setConfirmReset] = useState(null);
    const [busy, setBusy] = useState(false);

    const startEditPlan = () => setEditPlan(detailItem?.subscriptionPlan || "FREE");
    const cancelEditPlan = () => { setEditPlan(null); setConfirmPlan(false); };

    const savePlan = async () => {
        if (!editPlan || !detailItem) return;
        setBusy(true);
        try {
            await apiPatch(`/api/admin/users/${detailItem.id}/plan`, { subscriptionPlan: editPlan });
            showToast("Plan updated!", "success");
            setEditPlan(null);
            setConfirmPlan(false);
            onRefresh(detailItem.type, detailItem.id);
        } catch (err) {
            showToast(err?.message || "Failed to update plan", "error");
        } finally {
            setBusy(false);
        }
    };

    const handleReset = async (kind) => {
        if (!detailItem) return;
        setBusy(true);
        try {
            await apiPost(`/api/admin/users/${detailItem.id}/reset-${kind}-usage`);
            showToast(`${kind === "email" ? "Email" : "Download"} usage reset!`, "success");
            setConfirmReset(null);
            onRefresh(detailItem.type, detailItem.id);
        } catch (err) {
            showToast(err?.message || "Reset failed", "error");
        } finally {
            setBusy(false);
        }
    };
    return (
        <div className="admin-overlay" onClick={onClose}>
            <div className="admin-detail-modal" onClick={(e) => e.stopPropagation()}>
                {detailLoading ? (
                    <div className="admin-loading">Loading details...</div>
                ) : detailItem && (
                    <>
                        <div className="admin-detail-header">
                            <h3>
                                <FiEye size={16} />
                                {detailItem.type === "users" && `User #${detailItem.id}`}
                                {detailItem.type === "groups" && `Group #${detailItem.id}`}
                                {detailItem.type === "tasks" && `Task #${detailItem.id}`}
                                {detailItem.type === "comments" && `Comment #${detailItem.id}`}
                            </h3>
                            <div className="admin-detail-header-actions">
                                <button
                                    className="admin-detail-refresh"
                                    onClick={() => onRefresh(detailItem.type, detailItem.id)}
                                    title="Refresh"
                                >
                                    <FiRefreshCw size={15} />
                                </button>
                                <button className="admin-detail-close" onClick={onClose}>
                                    <FiX size={18} />
                                </button>
                            </div>
                        </div>

                        <div className="admin-detail-body">
                            { }
                            {detailItem.type === "users" && (
                                <div className="admin-detail-grid">
                                    {(detailItem.imgUrl || detailItem.defaultImgUrl) && (
                                        <>
                                            <label>Image</label>
                                            <img
                                                src={blobUrl(detailItem.imgUrl || detailItem.defaultImgUrl)}
                                                alt="User"
                                                className="admin-user-avatar"
                                            />
                                        </>
                                    )}
                                    <label>Name</label><span>{detailItem.name || "-"}</span>
                                    <label>Email</label><span>{detailItem.email || "-"}</span>
                                    <label>System Role</label><span>{detailItem.systemRole || "-"}</span>
                                    <label>Plan</label>
                                    <span>
                                        {editPlan !== null ? (
                                            <>
                                                <select value={editPlan} onChange={(e) => setEditPlan(e.target.value)} disabled={busy} className="admin-plan-select">
                                                    {PLANS.map((p) => <option key={p} value={p}>{p}</option>)}
                                                </select>
                                                {!confirmPlan ? (
                                                    <>
                                                        <button className="admin-btn-sm admin-btn-primary-sm" onClick={() => setConfirmPlan(true)} disabled={busy || editPlan === detailItem.subscriptionPlan}>Save</button>
                                                        <button className="admin-btn-sm" onClick={cancelEditPlan} disabled={busy}>Cancel</button>
                                                    </>
                                                ) : (
                                                    <>
                                                        <span className="admin-confirm-text">Change to {editPlan}?</span>
                                                        <button className="admin-btn-sm admin-btn-primary-sm" onClick={savePlan} disabled={busy}>{busy ? "…" : "Confirm"}</button>
                                                        <button className="admin-btn-sm" onClick={() => setConfirmPlan(false)} disabled={busy}>Back</button>
                                                    </>
                                                )}
                                            </>
                                        ) : (
                                            <>
                                                {detailItem.subscriptionPlan || "-"}
                                                <button className="admin-btn-sm" onClick={startEditPlan} style={{ marginLeft: 8 }}>Edit</button>
                                            </>
                                        )}
                                    </span>
                                    <label>Email Notif.</label><span>{detailItem.allowEmailNotification ? "Yes" : "No"}</span>
                                    <label>Org</label><span>{detailItem.isOrg ? "Yes" : "No"}</span>
                                    <label>Tenant ID</label><span>{detailItem.tenantId || "-"}</span>
                                    <label>Last Active</label><span>{formatDate(detailItem.lastActiveAt, "-")}</span>

                                    <div className="admin-thresholds-section" style={{ gridColumn: "1 / -1" }}>
                                        <h4>Thresholds</h4>
                                        <ThresholdBar label="Storage" used={detailItem.usedStorageBytes ?? 0} total={detailItem.storageBudgetBytes} format={formatFileSize} />
                                        <ThresholdBar label="Downloads" used={detailItem.usedDownloadBytesMonth ?? 0} total={detailItem.downloadBudgetBytes} format={formatFileSize} />
                                        <ThresholdBar label="Emails" used={detailItem.usedEmailsMonth ?? 0} total={detailItem.emailsPerMonth} />
                                        <ThresholdBar label="Image Scans" used={detailItem.usedImageScansMonth ?? 0} total={detailItem.imageScansPerMonth} />

                                        <div className="admin-reset-actions">
                                            {confirmReset === "email" ? (
                                                <>
                                                    <span className="admin-confirm-text">Reset email usage?</span>
                                                    <button className="admin-btn-sm admin-btn-primary-sm" onClick={() => handleReset("email")} disabled={busy}>{busy ? "…" : "Confirm"}</button>
                                                    <button className="admin-btn-sm" onClick={() => setConfirmReset(null)} disabled={busy}>Cancel</button>
                                                </>
                                            ) : (
                                                <button className="admin-btn-sm" onClick={() => setConfirmReset("email")} disabled={busy}>Reset email usage</button>
                                            )}
                                            {confirmReset === "download" ? (
                                                <>
                                                    <span className="admin-confirm-text">Reset download usage?</span>
                                                    <button className="admin-btn-sm admin-btn-primary-sm" onClick={() => handleReset("download")} disabled={busy}>{busy ? "…" : "Confirm"}</button>
                                                    <button className="admin-btn-sm" onClick={() => setConfirmReset(null)} disabled={busy}>Cancel</button>
                                                </>
                                            ) : (
                                                <button className="admin-btn-sm" onClick={() => setConfirmReset("download")} disabled={busy}>Reset download usage</button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )}

                            { }
                            {detailItem.type === "groups" && (
                                <>
                                    <div className="admin-detail-grid">
                                        <label>Name</label><span>{detailItem.name || "-"}</span>
                                        <label>Description</label><span>{detailItem.description || "-"}</span>
                                        <label>Announcement</label><span>{detailItem.announcement || "-"}</span>
                                        <label>Email Notif.</label><span>{detailItem.allowEmailNotification ? "Yes" : "No"}</span>
                                        <label>Owner</label><span>{detailItem.ownerName} ({detailItem.ownerEmail})</span>
                                        <label>Created</label><span>{formatDate(detailItem.createdAt, "-")}</span>
                                    </div>
                                    {detailItem.members && detailItem.members.length > 0 && (
                                        <div className="admin-detail-sub">
                                            <h4>Members ({detailItem.members.length})</h4>
                                            <table className="admin-sub-table">
                                                <thead><tr><th>User ID</th><th>Name</th><th>Role</th></tr></thead>
                                                <tbody>
                                                    {detailItem.members.map((m) => (
                                                        <tr key={m.id}><td>{m.userId}</td><td>{m.userName || "-"}</td><td>{m.role}</td></tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </>
                            )}

                            { }
                            {detailItem.type === "tasks" && (
                                <>
                                    <div className="admin-detail-grid">
                                        <label>Title</label><span>{detailItem.title || "-"}</span>
                                        <label>Description</label><span className="admin-cell-clamp-long">{detailItem.description || "-"}</span>
                                        <label>State</label><span>{detailItem.taskState || "-"}</span>
                                        <label>Priority</label><span>{detailItem.priority ?? "-"}</span>
                                        <label>Due Date</label><span>{detailItem.dueDate ? formatDate(detailItem.dueDate, "-") : "-"}</span>
                                        <label>Group</label><span>{detailItem.groupName || `#${detailItem.groupId}`}</span>
                                        <label>Creator</label><span>{detailItem.creatorNameSnapshot || "-"}</span>
                                        <label>Reviewed by</label><span>{detailItem.reviewedBy || "-"}</span>
                                        <label>Review decision</label><span>{detailItem.reviewersDecision || "-"}</span>
                                        {detailItem.reviewComment && <><label>Review comment</label><span>{detailItem.reviewComment}</span></>}
                                    </div>
                                    { }
                                    {detailItem.participants && detailItem.participants.length > 0 && (
                                        <div className="admin-detail-sub">
                                            <h4>Participants ({detailItem.participants.length})</h4>
                                            <table className="admin-sub-table">
                                                <thead><tr><th>User ID</th><th>Name</th><th>Role</th></tr></thead>
                                                <tbody>
                                                    {detailItem.participants.map((p) => (
                                                        <tr key={p.id}><td>{p.userId}</td><td>{p.userName || "-"}</td><td>{p.role}</td></tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                    { }
                                    {detailItem.creatorFiles && detailItem.creatorFiles.length > 0 && (
                                        <div className="admin-detail-sub">
                                            <h4>Creator Files ({detailItem.creatorFiles.length})</h4>
                                            <ul className="admin-file-list">
                                                {detailItem.creatorFiles.map((f) => (
                                                    <li key={f.id}>
                                                        <button
                                                            className="admin-file-link"
                                                            onClick={() => onDownload(detailItem.id, f.id, f.name || f.fileUrl, false)}
                                                            disabled={downloadingId === f.id}
                                                        >
                                                            <FiDownload size={13} /> {f.name || f.fileUrl}
                                                            {downloadingId === f.id && <span className="admin-file-downloading">↓</span>}
                                                        </button>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                    { }
                                    {detailItem.assigneeFiles && detailItem.assigneeFiles.length > 0 && (
                                        <div className="admin-detail-sub">
                                            <h4>Assignee Files ({detailItem.assigneeFiles.length})</h4>
                                            <ul className="admin-file-list">
                                                {detailItem.assigneeFiles.map((f) => (
                                                    <li key={f.id}>
                                                        <button
                                                            className="admin-file-link"
                                                            onClick={() => onDownload(detailItem.id, f.id, f.name || f.fileUrl, true)}
                                                            disabled={downloadingId === f.id}
                                                        >
                                                            <FiDownload size={13} /> {f.name || f.fileUrl}
                                                            {downloadingId === f.id && <span className="admin-file-downloading">↓</span>}
                                                        </button>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                </>
                            )}

                            { }
                            {detailItem.type === "comments" && (
                                <div className="admin-detail-grid">
                                    <label>Comment</label><span className="admin-cell-clamp-long">{detailItem.comment || "-"}</span>
                                    <label>Author</label><span>{detailItem.creatorName || "-"} {detailItem.creatorEmail ? `(${detailItem.creatorEmail})` : ""}</span>
                                    <label>Task</label><span>{detailItem.taskTitle || "-"} (#{detailItem.taskId})</span>
                                    <label>Group</label><span>{detailItem.groupName || "-"} {detailItem.groupId ? `(#${detailItem.groupId})` : ""}</span>
                                    <label>Created</label><span>{formatDate(detailItem.createdAt, "-")}</span>
                                </div>
                            )}
                        </div>

                        { }
                        <div className="admin-detail-actions">
                            <button
                                className="admin-btn-danger"
                                onClick={() => onRequestDelete({ type: detailItem.type, id: detailItem.id, label: detailItem.name || detailItem.title || `#${detailItem.id}` })}
                            >
                                <FiTrash2 size={14} /> Delete
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
