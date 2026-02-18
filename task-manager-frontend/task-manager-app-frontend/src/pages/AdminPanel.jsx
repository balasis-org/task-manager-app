import { useContext, useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { FiUsers, FiLayers, FiCheckSquare, FiMessageSquare, FiTrash2, FiChevronLeft, FiChevronRight, FiSearch } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { apiGet, apiDelete } from "@assets/js/apiClient.js";
import { useToast } from "@context/ToastContext";
import "@styles/pages/AdminPanel.css";

const TABS = [
    { key: "users",    label: "Users",    icon: <FiUsers size={15} /> },
    { key: "groups",   label: "Groups",   icon: <FiLayers size={15} /> },
    { key: "tasks",    label: "Tasks",    icon: <FiCheckSquare size={15} /> },
    { key: "comments", label: "Comments", icon: <FiMessageSquare size={15} /> },
];

const PAGE_SIZE = 15;

export default function AdminPanel() {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const showToast = useToast();

    const [tab, setTab] = useState("users");
    const [data, setData] = useState(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [searchQ, setSearchQ] = useState("");
    const [confirmDelete, setConfirmDelete] = useState(null); // { type, id, label }

    // guard — redirect non-admins
    useEffect(() => {
        if (user && user.systemRole !== "ADMIN") {
            navigate("/dashboard", { replace: true });
        }
    }, [user, navigate]);

    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            let url = `/api/admin/${tab}?page=${page}&size=${PAGE_SIZE}&sort=id,desc`;
            if (tab === "users" && searchQ.trim()) {
                url += `&q=${encodeURIComponent(searchQ.trim())}`;
            }
            const res = await apiGet(url);
            setData(res);
        } catch (err) {
            showToast("Failed to load data", "error");
            setData(null);
        } finally {
            setLoading(false);
        }
    }, [tab, page, searchQ]);

    useEffect(() => {
        if (!user || user.systemRole !== "ADMIN") return;
        fetchData();
    }, [fetchData, user]);

    // when switching tabs reset page and search
    const switchTab = (key) => {
        setTab(key);
        setPage(0);
        setSearchQ("");
        setConfirmDelete(null);
    };

    const handleDelete = async () => {
        if (!confirmDelete) return;
        const { type, id } = confirmDelete;
        try {
            await apiDelete(`/api/admin/${type}/${id}`);
            showToast("Deleted successfully", "success");
            setConfirmDelete(null);
            fetchData();
        } catch (err) {
            const msg = err?.message || "Delete failed";
            showToast(msg, "error");
            setConfirmDelete(null);
        }
    };

    if (!user || user.systemRole !== "ADMIN") return null;

    const items = data?.content || [];
    const totalPages = data?.totalPages || 0;
    const totalElements = data?.totalElements || 0;

    return (
        <div className="admin-panel">
            <header className="admin-header">
                <h1>Admin Panel</h1>
                <span className="admin-record-count">{totalElements} records</span>
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

            {/* search (users only) */}
            {tab === "users" && (
                <div className="admin-search">
                    <FiSearch size={14} />
                    <input
                        type="text"
                        placeholder="Search by name or email..."
                        value={searchQ}
                        onChange={(e) => { setSearchQ(e.target.value); setPage(0); }}
                    />
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
                                {tab === "comments" && <><th>ID</th><th>Comment</th><th>Author</th><th>Task</th><th>Created</th><th></th></>}
                            </tr>
                        </thead>
                        <tbody>
                            {items.map((item) => (
                                <tr key={item.id}>
                                    {tab === "users" && (
                                        <>
                                            <td>{item.id}</td>
                                            <td>{item.name}</td>
                                            <td>{item.email}</td>
                                            <td><span className={`admin-badge ${item.systemRole === "ADMIN" ? "admin" : "user"}`}>{item.systemRole}</span></td>
                                            <td>{item.subscriptionPlan}</td>
                                            <td>{fmtDate(item.lastActiveAt)}</td>
                                            <td>
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
                                            <td>
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
                                            <td>
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
                                            <td>{fmtDate(item.createdAt)}</td>
                                            <td>
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

            {/* confirm modal */}
            {confirmDelete && (
                <div className="admin-overlay" onClick={() => setConfirmDelete(null)}>
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
