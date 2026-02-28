import { LIMITS } from "@assets/js/inputValidation";
import "@styles/task/TaskBody.css";

export default function TaskBody({
    editing, task, editTitle, editDesc,
    onTitleChange, onDescChange, onSave, onCancel,
    children,
}) {
    return (
        <div className="task-body">
            {editing ? (
                <>
                    <input
                        className="task-title-input"
                        value={editTitle}
                        onChange={(e) => onTitleChange(e.target.value)}
                        placeholder="Task title"
                        maxLength={LIMITS.TASK_TITLE}
                    />
                    <span className="char-count">
                        {editTitle.length}/{LIMITS.TASK_TITLE}
                    </span>

                    <textarea
                        className="task-desc-input"
                        value={editDesc}
                        onChange={(e) => onDescChange(e.target.value)}
                        placeholder="Task description"
                        rows={6}
                        maxLength={LIMITS.TASK_DESCRIPTION}
                    />
                    <span className="char-count">
                        {editDesc.length}/{LIMITS.TASK_DESCRIPTION}
                    </span>

                    <div className="task-edit-actions">
                        <button className="btn-primary" onClick={onSave}>Save</button>
                        <button className="btn-secondary" onClick={onCancel}>Cancel</button>
                    </div>
                </>
            ) : (
                <>
                    <h1 className="task-title">{task.title || "Untitled"}</h1>
                    <p className="task-description">
                        {task.description || <em>No description</em>}
                    </p>
                </>
            )}

            {children}
        </div>
    );
}
