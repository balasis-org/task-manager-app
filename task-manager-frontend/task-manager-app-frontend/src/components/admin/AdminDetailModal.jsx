// Keeping this as one component with if/else per type rather than splitting
// into UserDetail, GroupDetail, etc. — the shared overlay/header/actions don't
// justify four nearly-identical wrapper files.
import { FiEye, FiRefreshCw, FiX, FiTrash2, FiDownload } from "react-icons/fi";
import "@styles/admin/AdminDetailModal.css";

export default function AdminDetailModal({
    detailItem, detailLoading, onClose, onRefresh,
    onRequestDelete, blobUrl, onDownload, downloadingId, formatDate,
}) {
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
                                    <label>Name</label><span>{detailItem.name || "—"}</span>
                                    <label>Email</label><span>{detailItem.email || "—"}</span>
                                    <label>System Role</label><span>{detailItem.systemRole || "—"}</span>
                                    <label>Plan</label><span>{detailItem.subscriptionPlan || "—"}</span>
                                    <label>Email Notif.</label><span>{detailItem.allowEmailNotification ? "Yes" : "No"}</span>
                                    <label>Org</label><span>{detailItem.isOrg ? "Yes" : "No"}</span>
                                    <label>Tenant ID</label><span>{detailItem.tenantId || "—"}</span>
                                    <label>Last Active</label><span>{formatDate(detailItem.lastActiveAt, "—")}</span>
                                </div>
                            )}

                            { }
                            {detailItem.type === "groups" && (
                                <>
                                    <div className="admin-detail-grid">
                                        <label>Name</label><span>{detailItem.name || "—"}</span>
                                        <label>Description</label><span>{detailItem.description || "—"}</span>
                                        <label>Announcement</label><span>{detailItem.announcement || "—"}</span>
                                        <label>Email Notif.</label><span>{detailItem.allowEmailNotification ? "Yes" : "No"}</span>
                                        <label>Owner</label><span>{detailItem.ownerName} ({detailItem.ownerEmail})</span>
                                        <label>Created</label><span>{formatDate(detailItem.createdAt, "—")}</span>
                                    </div>
                                    {detailItem.members && detailItem.members.length > 0 && (
                                        <div className="admin-detail-sub">
                                            <h4>Members ({detailItem.members.length})</h4>
                                            <table className="admin-sub-table">
                                                <thead><tr><th>User ID</th><th>Name</th><th>Role</th></tr></thead>
                                                <tbody>
                                                    {detailItem.members.map((m) => (
                                                        <tr key={m.id}><td>{m.userId}</td><td>{m.userName || "—"}</td><td>{m.role}</td></tr>
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
                                        <label>Title</label><span>{detailItem.title || "—"}</span>
                                        <label>Description</label><span className="admin-cell-clamp-long">{detailItem.description || "—"}</span>
                                        <label>State</label><span>{detailItem.taskState || "—"}</span>
                                        <label>Priority</label><span>{detailItem.priority ?? "—"}</span>
                                        <label>Due Date</label><span>{detailItem.dueDate ? formatDate(detailItem.dueDate, "—") : "—"}</span>
                                        <label>Group</label><span>{detailItem.groupName || `#${detailItem.groupId}`}</span>
                                        <label>Creator</label><span>{detailItem.creatorNameSnapshot || "—"}</span>
                                        <label>Reviewed by</label><span>{detailItem.reviewedBy || "—"}</span>
                                        <label>Review decision</label><span>{detailItem.reviewersDecision || "—"}</span>
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
                                                        <tr key={p.id}><td>{p.userId}</td><td>{p.userName || "—"}</td><td>{p.role}</td></tr>
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
                                    <label>Comment</label><span className="admin-cell-clamp-long">{detailItem.comment || "—"}</span>
                                    <label>Author</label><span>{detailItem.creatorName || "—"} {detailItem.creatorEmail ? `(${detailItem.creatorEmail})` : ""}</span>
                                    <label>Task</label><span>{detailItem.taskTitle || "—"} (#{detailItem.taskId})</span>
                                    <label>Group</label><span>{detailItem.groupName || "—"} {detailItem.groupId ? `(#${detailItem.groupId})` : ""}</span>
                                    <label>Created</label><span>{formatDate(detailItem.createdAt, "—")}</span>
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
