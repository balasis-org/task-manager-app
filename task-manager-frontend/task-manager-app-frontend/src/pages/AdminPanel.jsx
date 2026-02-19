import { useContext, useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
    FiUsers, FiLayers, FiCheckSquare, FiMessageSquare,
    FiTrash2, FiChevronLeft, FiChevronRight, FiSearch,
    FiX, FiEye, FiFilter, FiDownload, FiRefreshCw
} from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { apiGet, apiDelete } from "@assets/js/apiClient.js";
import { useToast } from "@context/ToastContext";
import blobBase from "@blobBase";
import "@styles/pages/AdminPanel.css";

const TABS = [
    { key: "users",    label: "Users",    icon: <FiUsers size={15} /> },
    { key: "groups",   label: "Groups",   icon: <FiLayers size={15} /> },
    { key: "tasks",    label: "Tasks",    icon: <FiCheckSquare size={15} /> },
    { key: "comments", label: "Comments", icon: <FiMessageSquare size={15} /> },
];

const PAGE_SIZE = 15;
const DEBOUNCE_MS = 400;

function useDebounce(value, delay) {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const t = setTimeout(() => setDebounced(value), delay);
        return () => clearTimeout(t);
    }, [value, delay]);
    return debounced;
}

export default function AdminPanel() {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const showToast = useToast();

    const [tab, setTab] = useState("users");
    const [data, setData] = useState(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);

    // search / filters
    const [searchQ, setSearchQ] = useState("");
    const debouncedQ = useDebounce(searchQ, DEBOUNCE_MS);

    // Comment multi-filters (applied on button click, not on change)
    const [commentFilters, setCommentFilters] = useState({ taskId: "", groupId: "", creatorId: "" });
    const [appliedCommentFilters, setAppliedCommentFilters] = useState({ taskId: "", groupId: "", creatorId: "" });

    // Detail modal (read-only)
    const [detailItem, setDetailItem] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);

    // Delete confirm
    const [confirmDelete, setConfirmDelete] = useState(null);

    // File download
    const [downloadingId, setDownloadingId] = useState(null);

    // guard
    useEffect(() => {
        if (user && user.systemRole !== "ADMIN") navigate("/dashboard", { replace: true });
    }, [user, navigate]);

    /* ── fetch list ── */
    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            let url = `/api/admin/${tab}?page=${page}&size=${PAGE_SIZE}&sort=id,desc`;
            if ((tab === "users" || tab === "groups" || tab === "tasks") && debouncedQ.trim()) {
                url += `&q=${encodeURIComponent(debouncedQ.trim())}`;
            }
            if (tab === "comments") {
                if (appliedCommentFilters.taskId) url += `&taskId=${appliedCommentFilters.taskId}`;
                if (appliedCommentFilters.groupId) url += `&groupId=${appliedCommentFilters.groupId}`;
                if (appliedCommentFilters.creatorId) url += `&creatorId=${appliedCommentFilters.creatorId}`;
            }
            const res = await apiGet(url);
            setData(res);
        } catch {
            showToast("Failed to load data", "error");
            setData(null);
        } finally {
            setLoading(false);
        }
    }, [tab, page, debouncedQ, appliedCommentFilters, showToast]);

    useEffect(() => {
        if (!user || user.systemRole !== "ADMIN") return;
        fetchData();
    }, [fetchData, user]);

    const switchTab = (key) => {
        setTab(key);
        setPage(0);
        setSearchQ("");
        setCommentFilters({ taskId: "", groupId: "", creatorId: "" });
        setAppliedCommentFilters({ taskId: "", groupId: "", creatorId: "" });
        setConfirmDelete(null);
        setDetailItem(null);
    };

    /* ── detail fetch ── */
    const openDetail = async (type, id) => {
        setDetailLoading(true);
        try {
            const item = await apiGet(`/api/admin/${type}/${id}`);
            setDetailItem({ type, ...item });
        } catch {
            showToast("Failed to load details", "error");
        } finally {
            setDetailLoading(false);
        }
    };

    /* ── delete ── */
    const handleDelete = async () => {
        if (!confirmDelete) return;
        const { type, id } = confirmDelete;
        try {
            await apiDelete(`/api/admin/${type}/${id}`);
            showToast("Deleted", "success");
            setConfirmDelete(null);
            if (detailItem?.id === id) setDetailItem(null);
            fetchData();
        } catch (err) {
            showToast(err?.message || "Delete failed", "error");
            setConfirmDelete(null);
        }
    };

    if (!user || user.systemRole !== "ADMIN") return null;

    /* ── admin file download ── */
    async function handleAdminDownload(taskId, fileId, filename, isAssignee) {
        setDownloadingId(fileId);
        try {
            const endpoint = isAssignee
                ? `/api/admin/tasks/${taskId}/assignee-files/${fileId}/download`
                : `/api/admin/tasks/${taskId}/files/${fileId}/download`;
            const blob = await apiGet(endpoint, { responseType: "blob" });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = filename;
            a.click();
            URL.revokeObjectURL(url);
        } catch (err) {
            showToast(err?.message || "Failed to download file", "error");
        } finally {
            setDownloadingId(null);
        }
    }

    const items = data?.content || [];
    const totalPages = data?.totalPages || 0;
    const totalElements = data?.totalElements || 0;

    return (
        <div className="admin-panel">
            <header className="admin-header">
                <h1>Admin Panel</h1>
                <span className="admin-record-count">{totalElements} records</span>
                <button className="admin-refresh-btn" onClick={fetchData} title="Refresh">
                    <FiRefreshCw size={15} />
                </button>
            </header>

            {/* tabs */}
            <div className="admin-tabs">
                {TABS.map((t) => (
                    <button
                        key={t.key}
                        className={`admin-tab${tab === t.key ? " active" : ""}`}
                        onClick={() => switchTab(t.key)}
                    >
                        {t.icon}
                        <span>{t.label}</span>
                    </button>
                ))}
            </div>

            {/* search bar (users, groups, tasks) */}
            {(tab === "users" || tab === "groups" || tab === "tasks") && (
                <div className="admin-search">
                    <FiSearch size={14} />
                    <input
                        type="text"
                        placeholder={
                            tab === "users" ? "Search by name or email..." :
                            tab === "groups" ? "Search by group or owner name..." :
                            "Search by title or group name..."
                        }
                        value={searchQ}
                        onChange={(e) => { setSearchQ(e.target.value); setPage(0); }}
                    />
                </div>
            )}

            {/* comment multi-filter */}
            {tab === "comments" && (
                <div className="admin-comment-filters">
                    <FiFilter size={14} />
                    <input
                        type="number"
                        placeholder="Task ID"
                        value={commentFilters.taskId}
                        onChange={(e) => setCommentFilters((f) => ({ ...f, taskId: e.target.value }))}
                    />
                    <input
                        type="number"
                        placeholder="Group ID"
                        value={commentFilters.groupId}
                        onChange={(e) => setCommentFilters((f) => ({ ...f, groupId: e.target.value }))}
                    />
                    <input
                        type="number"
                        placeholder="Creator ID"
                        value={commentFilters.creatorId}
                        onChange={(e) => setCommentFilters((f) => ({ ...f, creatorId: e.target.value }))}
                    />
                    <button
                        className="admin-filter-btn"
                        onClick={() => { setAppliedCommentFilters({ ...commentFilters }); setPage(0); }}
                    >
                        Search
                    </button>
                    {(appliedCommentFilters.taskId || appliedCommentFilters.groupId || appliedCommentFilters.creatorId) && (
                        <button
                            className="admin-filter-clear"
                            onClick={() => {
                                const empty = { taskId: "", groupId: "", creatorId: "" };
                                setCommentFilters(empty);
                                setAppliedCommentFilters(empty);
                                setPage(0);
                            }}
                        >
                            Clear
                        </button>
                    )}
                </div>
            )}

            {/* table */}
            <div className="admin-table-wrap">
                {loading ? (
                    <div className="admin-loading">Loading...</div>
                ) : items.length === 0 ? (
                    <div className="admin-empty">No records found.</div>
                ) : (
                    <table className="admin-table">
                        <thead>
                            <tr>
                                {tab === "users" && <><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Plan</th><th>Last Active</th><th></th></>}
                                {tab === "groups" && <><th>ID</th><th>Name</th><th>Owner</th><th>Members</th><th>Tasks</th><th>Created</th><th></th></>}
                                {tab === "tasks" && <><th>ID</th><th>Title</th><th>State</th><th>Group</th><th>Creator</th><th>Due</th><th></th></>}
                                {tab === "comments" && <><th>ID</th><th>Comment</th><th>Author</th><th>Task</th><th>Group</th><th>Created</th><th></th></>}
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((item) => (
                                <tr key={item.id} className="admin-row-clickable" onClick={() => openDetail(tab, item.id)}>
                                    {tab === "users" && (
                                        <>
                                            <td>{item.id}</td>
                                            <td>{item.name}</td>
                                            <td>{item.email}</td>
                                            <td><span className={`admin-badge ${item.systemRole === "ADMIN" ? "admin" : "user"}`}>{item.systemRole}</span></td>
                                            <td>{item.subscriptionPlan}</td>
                                            <td>{fmtDate(item.lastActiveAt)}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                {item.systemRole !== "ADMIN" && (
                                                    <button className="admin-delete-btn" onClick={() => setConfirmDelete({ type: "users", id: item.id, label: item.name || item.email })} title="Delete user">
                                                        <FiTrash2 size={14} />
                                                    </button>
                                                )}
                                            </td>
                                        </>
                                    )}
                                    {tab === "groups" && (
                                        <>
                                            <td>{item.id}</td>
                                            <td>{item.name}</td>
                                            <td>{item.ownerName || item.ownerEmail}</td>
                                            <td>{item.memberCount}</td>
                                            <td>{item.taskCount}</td>
                                            <td>{fmtDate(item.createdAt)}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                <button className="admin-delete-btn" onClick={() => setConfirmDelete({ type: "groups", id: item.id, label: item.name })} title="Delete group">
                                                    <FiTrash2 size={14} />
                                                </button>
                                            </td>
                                        </>
                                    )}
                                    {tab === "tasks" && (
                                        <>
                                            <td>{item.id}</td>
                                            <td className="admin-cell-clamp">{item.title}</td>
                                            <td><span className={`admin-badge state-${(item.taskState || "").toLowerCase()}`}>{item.taskState}</span></td>
                                            <td>{item.groupName || `#${item.groupId}`}</td>
                                            <td>{item.creatorNameSnapshot || "—"}</td>
                                            <td>{fmtDate(item.dueDate)}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                <button className="admin-delete-btn" onClick={() => setConfirmDelete({ type: "tasks", id: item.id, label: item.title })} title="Delete task">
                                                    <FiTrash2 size={14} />
                                                </button>
                                            </td>
                                        </>
                                    )}
                                    {tab === "comments" && (
                                        <>
                                            <td>{item.id}</td>
                                            <td className="admin-cell-clamp">{item.comment}</td>
                                            <td>{item.creatorName || "—"}</td>
                                            <td>{item.taskTitle || `#${item.taskId}`}</td>
                                            <td>{item.groupName || (item.groupId ? `#${item.groupId}` : "—")}</td>
                                            <td>{fmtDate(item.createdAt)}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                <button className="admin-delete-btn" onClick={() => setConfirmDelete({ type: "comments", id: item.id, label: `Comment #${item.id}` })} title="Delete comment">
                                                    <FiTrash2 size={14} />
                                                </button>
                                            </td>
                                        </>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* pagination */}
            {totalPages > 1 && (
                <div className="admin-pagination">
                    <button disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
                        <FiChevronLeft size={16} />
                    </button>
                    <span>{page + 1} / {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                        <FiChevronRight size={16} />
                    </button>
                </div>
            )}

            {/* ── Detail / Edit Modal ── */}
            {(detailItem || detailLoading) && (
                <div className="admin-overlay" onClick={() => setDetailItem(null)}>
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
                                            onClick={() => openDetail(detailItem.type, detailItem.id)}
                                            title="Refresh"
                                        >
                                            <FiRefreshCw size={15} />
                                        </button>
                                        <button className="admin-detail-close" onClick={() => setDetailItem(null)}>
                                            <FiX size={18} />
                                        </button>
                                    </div>
                                </div>

                                <div className="admin-detail-body">
                                    {/* ── USER detail (read-only) ── */}
                                    {detailItem.type === "users" && (
                                        <div className="admin-detail-grid">
                                            {(detailItem.imgUrl || detailItem.defaultImgUrl) && (
                                                <>
                                                    <label>Image</label>
                                                    <img
                                                        src={blobBase + (detailItem.imgUrl || detailItem.defaultImgUrl)}
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
                                            <label>Last Active</label><span>{fmtDate(detailItem.lastActiveAt)}</span>
                                        </div>
                                    )}

                                    {/* ── GROUP detail (read-only) ── */}
                                    {detailItem.type === "groups" && (
                                        <>
                                            <div className="admin-detail-grid">
                                                <label>Name</label><span>{detailItem.name || "—"}</span>
                                                <label>Description</label><span>{detailItem.description || "—"}</span>
                                                <label>Announcement</label><span>{detailItem.announcement || "—"}</span>
                                                <label>Email Notif.</label><span>{detailItem.allowEmailNotification ? "Yes" : "No"}</span>
                                                <label>Owner</label><span>{detailItem.ownerName} ({detailItem.ownerEmail})</span>
                                                <label>Created</label><span>{fmtDate(detailItem.createdAt)}</span>
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

                                    {/* ── TASK detail (read-only) ── */}
                                    {detailItem.type === "tasks" && (
                                        <>
                                            <div className="admin-detail-grid">
                                                <label>Title</label><span>{detailItem.title || "—"}</span>
                                                <label>Description</label><span className="admin-cell-clamp-long">{detailItem.description || "—"}</span>
                                                <label>State</label><span>{detailItem.taskState || "—"}</span>
                                                <label>Priority</label><span>{detailItem.priority ?? "—"}</span>
                                                <label>Due Date</label><span>{detailItem.dueDate ? fmtDate(detailItem.dueDate) : "—"}</span>
                                                <label>Group</label><span>{detailItem.groupName || `#${detailItem.groupId}`}</span>
                                                <label>Creator</label><span>{detailItem.creatorNameSnapshot || "—"}</span>
                                                <label>Reviewed by</label><span>{detailItem.reviewedBy || "—"}</span>
                                                <label>Review decision</label><span>{detailItem.reviewersDecision || "—"}</span>
                                                {detailItem.reviewComment && <><label>Review comment</label><span>{detailItem.reviewComment}</span></>}
                                            </div>
                                            {/* participants */}
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
                                            {/* creator files */}
                                            {detailItem.creatorFiles && detailItem.creatorFiles.length > 0 && (
                                                <div className="admin-detail-sub">
                                                    <h4>Creator Files ({detailItem.creatorFiles.length})</h4>
                                                    <ul className="admin-file-list">
                                                        {detailItem.creatorFiles.map((f) => (
                                                            <li key={f.id}>
                                                                <button
                                                                    className="admin-file-link"
                                                                    onClick={() => handleAdminDownload(detailItem.id, f.id, f.name || f.fileUrl, false)}
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
                                            {/* assignee files */}
                                            {detailItem.assigneeFiles && detailItem.assigneeFiles.length > 0 && (
                                                <div className="admin-detail-sub">
                                                    <h4>Assignee Files ({detailItem.assigneeFiles.length})</h4>
                                                    <ul className="admin-file-list">
                                                        {detailItem.assigneeFiles.map((f) => (
                                                            <li key={f.id}>
                                                                <button
                                                                    className="admin-file-link"
                                                                    onClick={() => handleAdminDownload(detailItem.id, f.id, f.name || f.fileUrl, true)}
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

                                    {/* ── COMMENT detail (read-only) ── */}
                                    {detailItem.type === "comments" && (
                                        <div className="admin-detail-grid">
                                            <label>Comment</label><span className="admin-cell-clamp-long">{detailItem.comment || "—"}</span>
                                            <label>Author</label><span>{detailItem.creatorName || "—"} {detailItem.creatorEmail ? `(${detailItem.creatorEmail})` : ""}</span>
                                            <label>Task</label><span>{detailItem.taskTitle || "—"} (#{detailItem.taskId})</span>
                                            <label>Group</label><span>{detailItem.groupName || "—"} {detailItem.groupId ? `(#${detailItem.groupId})` : ""}</span>
                                            <label>Created</label><span>{fmtDate(detailItem.createdAt)}</span>
                                        </div>
                                    )}
                                </div>

                                {/* actions — delete only (no edit) */}
                                <div className="admin-detail-actions">
                                    <button
                                        className="admin-btn-danger"
                                        onClick={() => setConfirmDelete({ type: detailItem.type, id: detailItem.id, label: detailItem.name || detailItem.title || `#${detailItem.id}` })}
                                    >
                                        <FiTrash2 size={14} /> Delete
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}

            {/* confirm delete modal */}
            {confirmDelete && (
                <div className="admin-overlay" onClick={() => setConfirmDelete(null)} style={{ zIndex: 110 }}>
                    <div className="admin-confirm-modal" onClick={(e) => e.stopPropagation()}>
                        <h3>Confirm Delete</h3>
                        <p>Are you sure you want to delete <strong>{confirmDelete.label}</strong>?</p>
                        <p className="admin-confirm-warn">This action cannot be undone.</p>
                        <div className="admin-confirm-actions">
                            <button className="admin-btn-cancel" onClick={() => setConfirmDelete(null)}>Cancel</button>
                            <button className="admin-btn-danger" onClick={handleDelete}>Delete</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

function fmtDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
