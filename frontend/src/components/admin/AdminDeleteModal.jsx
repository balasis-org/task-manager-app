import "@styles/admin/AdminDeleteModal.css";

export default function AdminDeleteModal({ confirmDelete, onConfirm, onCancel }) {
    return (
        <div className="admin-overlay" onClick={onCancel} style={{ zIndex: 110 }}>
            <div className="admin-confirm-modal" onClick={(e) => e.stopPropagation()}>
                <h3>Confirm Delete</h3>
                <p>Are you sure you want to delete <strong>{confirmDelete.label}</strong>?</p>
                <p className="admin-confirm-warn">This action cannot be undone.</p>
                <div className="admin-confirm-actions">
                    <button className="admin-btn-cancel" onClick={onCancel}>Cancel</button>
                    <button className="admin-btn-danger" onClick={onConfirm}>Delete</button>
                </div>
            </div>
        </div>
    );
}
