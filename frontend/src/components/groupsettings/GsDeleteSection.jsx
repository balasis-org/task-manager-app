import { useState } from "react";
import { FiTrash2, FiAlertTriangle } from "react-icons/fi";
import { apiDelete } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import "@styles/groupsettings/GsDeleteSection.css";

export default function GsDeleteSection({ group, onDeleted }) {
    const showToast = useToast();
    const [confirmDelete, setConfirmDelete] = useState(false);
    const [deleting, setDeleting] = useState(false);

    async function handleDelete() {
        if (!group?.id) return; // shouldn't happen, but just in case
        setDeleting(true);
        try {
            await apiDelete(`/api/groups/${group.id}`);
            showToast("Group deleted", "success");
            if (onDeleted) onDeleted();
        } catch (err) {
            showToast(err?.message || "Failed to delete group");
        } finally {
            setDeleting(false);
        }
    }

    return (
        <section className="gs-section gs-section-danger">
            <div className="gs-section-header">
                <span className="gs-section-label">
                    <FiTrash2 size={14} className="gs-icon-danger" /> Delete group
                </span>
                {!confirmDelete && (
                    <button className="gs-danger-btn" onClick={() => setConfirmDelete(true)}>
                        Delete
                    </button>
                )}
            </div>
            {confirmDelete && (
                <div className="gs-confirm-box">
                    <FiAlertTriangle size={16} className="gs-icon-danger" />
                    <span>This action is permanent. All tasks and data will be lost.</span>
                    <div className="gs-field-actions">
                        <button className="gs-danger-btn" onClick={handleDelete} disabled={deleting}>
                            {deleting ? "Deleting…" : "Confirm delete"}
                        </button>
                        <button className="gs-cancel-btn" onClick={() => setConfirmDelete(false)}>
                            Cancel
                        </button>
                    </div>
                </div>
            )}
        </section>
    );
}
