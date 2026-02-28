import "@styles/task/TaskDeleteModal.css";

const TaskDeleteModal = ({ onConfirm, onCancel }) => (
    <div className="task-delete-overlay" onClick={onCancel}>
        <div className="task-delete-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Delete task?</h3>
            <p>
                This action cannot be undone. The task and all its files and
                comments will be permanently deleted.
            </p>
            <div className="task-delete-modal-actions">
                <button className="btn-danger" onClick={onConfirm}>Delete</button>
                <button className="btn-secondary" onClick={onCancel}>Cancel</button>
            </div>
        </div>
    </div>
);

export default TaskDeleteModal;
