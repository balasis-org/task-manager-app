import { useContext, useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { FiRefreshCw } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { apiGet, apiDelete } from "@assets/js/apiClient.js";
import { useToast } from "@context/ToastContext";
import { useBlobUrl } from "@context/BlobSasContext";
import { formatDate } from "@assets/js/formatDate";
import useDebounce from "@hooks/useDebounce";
import AdminTabBar from "@components/admin/AdminTabBar";
import AdminSearchBar from "@components/admin/AdminSearchBar";
import AdminDataTable from "@components/admin/AdminDataTable";
import AdminDetailModal from "@components/admin/AdminDetailModal";
import AdminDeleteModal from "@components/admin/AdminDeleteModal";
import "@styles/pages/AdminPanel.css";

const PAGE_SIZE = 15;
const DEBOUNCE_MS = 400;

export default function AdminPanel() {
    const { user } = useContext(AuthContext);
    const navigate = useNavigate();
    const showToast = useToast();
    const blobUrl = useBlobUrl();

    const [tab, setTab] = useState("users");
    const [data, setData] = useState(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);

    const [searchQ, setSearchQ] = useState("");
    const debouncedQ = useDebounce(searchQ, DEBOUNCE_MS);

    const [commentFilters, setCommentFilters] = useState({ taskId: "", groupId: "", creatorId: "" });
    const [appliedCommentFilters, setAppliedCommentFilters] = useState({ taskId: "", groupId: "", creatorId: "" });

    const [detailItem, setDetailItem] = useState(null);
    const [detailLoading, setDetailLoading] = useState(false);

    const [confirmDelete, setConfirmDelete] = useState(null);

    const [downloadingId, setDownloadingId] = useState(null);

    useEffect(() => {
        if (user && user.systemRole !== "ADMIN") navigate("/dashboard", { replace: true });
    }, [user, navigate]);

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

            <AdminTabBar activeTab={tab} onSwitch={switchTab} />

            <AdminSearchBar
                tab={tab}
                searchQ={searchQ}
                onSearchChange={(val) => { setSearchQ(val); setPage(0); }}
                commentFilters={commentFilters}
                onCommentFilterChange={setCommentFilters}
                appliedCommentFilters={appliedCommentFilters}
                onApplyFilters={() => { setAppliedCommentFilters({ ...commentFilters }); setPage(0); }}
                onClearFilters={() => {
                    const empty = { taskId: "", groupId: "", creatorId: "" };
                    setCommentFilters(empty);
                    setAppliedCommentFilters(empty);
                    setPage(0);
                }}
            />

            <AdminDataTable
                tab={tab}
                items={items}
                loading={loading}
                page={page}
                totalPages={totalPages}
                onPageChange={setPage}
                onOpenDetail={openDetail}
                onRequestDelete={setConfirmDelete}
                formatDate={formatDate}
            />

            {(detailItem || detailLoading) && (
                <AdminDetailModal
                    detailItem={detailItem}
                    detailLoading={detailLoading}
                    onClose={() => setDetailItem(null)}
                    onRefresh={openDetail}
                    onRequestDelete={setConfirmDelete}
                    blobUrl={blobUrl}
                    onDownload={handleAdminDownload}
                    downloadingId={downloadingId}
                    formatDate={formatDate}
                />
            )}

            {confirmDelete && (
                <AdminDeleteModal
                    confirmDelete={confirmDelete}
                    onConfirm={handleDelete}
                    onCancel={() => setConfirmDelete(null)}
                />
            )}
        </div>
    );
}
