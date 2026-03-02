// Comments tab gets its own filter row; everything else just uses a search box
import { FiSearch, FiFilter } from "react-icons/fi";
import "@styles/admin/AdminSearchBar.css";

export default function AdminSearchBar({
    tab, searchQ, onSearchChange,
    commentFilters, onCommentFilterChange,
    appliedCommentFilters, onApplyFilters, onClearFilters,
}) {
    if (tab === "comments") {
        const hasApplied = appliedCommentFilters.taskId || appliedCommentFilters.groupId || appliedCommentFilters.creatorId;
        return (
            <div className="admin-comment-filters">
                <FiFilter size={14} />
                <input
                    type="number" placeholder="Task ID"
                    value={commentFilters.taskId}
                    onChange={(e) => onCommentFilterChange({ ...commentFilters, taskId: e.target.value })}
                />
                <input
                    type="number" placeholder="Group ID"
                    value={commentFilters.groupId}
                    onChange={(e) => onCommentFilterChange({ ...commentFilters, groupId: e.target.value })}
                />
                <input
                    type="number" placeholder="Creator ID"
                    value={commentFilters.creatorId}
                    onChange={(e) => onCommentFilterChange({ ...commentFilters, creatorId: e.target.value })}
                />
                <button className="admin-filter-btn" onClick={onApplyFilters}>Search</button>
                {hasApplied && (
                    <button className="admin-filter-clear" onClick={onClearFilters}>Clear</button>
                )}
            </div>
        );
    }

    // comments already returned above - everything else is a simple search box
    return (
        <div className="admin-search">
            <FiSearch size={14} />
            <input
                type="text"
                placeholder={
                    tab === "users"  ? "Search by name or email..." :
                    tab === "groups" ? "Search by group or owner name..." :
                    "Search by title or group name..."
                }
                value={searchQ}
                onChange={(e) => onSearchChange(e.target.value)}
            />
        </div>
    );
}
