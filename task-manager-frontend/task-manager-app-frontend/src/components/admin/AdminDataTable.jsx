// One table for all admin tabs — columns switch based on `tab`.
// Intentionally inline JSX per tab rather than a column-config object;
// we tried the config approach and it was harder to read with mixed JSX badges/buttons.
import { FiTrash2, FiChevronLeft, FiChevronRight } from "react-icons/fi";
import "@styles/admin/AdminDataTable.css";

export default function AdminDataTable({
    tab, items, loading, page, totalPages,
    onPageChange, onOpenDetail, onRequestDelete, formatDate,
}) {
    return (
        <>
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
                                <tr key={item.id} className="admin-row-clickable" onClick={() => onOpenDetail(tab, item.id)}>
                                    {tab === "users" && (
                                        <>
                                            <td>{item.id}</td>
                                            <td>{item.name}</td>
                                            <td>{item.email}</td>
                                            <td><span className={`admin-badge ${item.systemRole === "ADMIN" ? "admin" : "user"}`}>{item.systemRole}</span></td>
                                            <td>{item.subscriptionPlan}</td>
                                            <td>{formatDate(item.lastActiveAt, "—")}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                {item.systemRole !== "ADMIN" && (
                                                    <button className="admin-delete-btn" onClick={() => onRequestDelete({ type: "users", id: item.id, label: item.name || item.email })} title="Delete user">
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
                                            <td>{formatDate(item.createdAt, "—")}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                <button className="admin-delete-btn" onClick={() => onRequestDelete({ type: "groups", id: item.id, label: item.name })} title="Delete group">
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
                                            <td>{formatDate(item.dueDate, "—")}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                <button className="admin-delete-btn" onClick={() => onRequestDelete({ type: "tasks", id: item.id, label: item.title })} title="Delete task">
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
                                            <td>{formatDate(item.createdAt, "—")}</td>
                                            <td onClick={(e) => e.stopPropagation()}>
                                                <button className="admin-delete-btn" onClick={() => onRequestDelete({ type: "comments", id: item.id, label: `Comment #${item.id}` })} title="Delete comment">
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

            {totalPages > 1 && (
                <div className="admin-pagination">
                    <button disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
                        <FiChevronLeft size={16} />
                    </button>
                    <span>{page + 1} / {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>
                        <FiChevronRight size={16} />
                    </button>
                </div>
            )}
        </>
    );
}
