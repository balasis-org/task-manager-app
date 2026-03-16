// State badge, or editable <select> when allowed
import "@styles/task/TaskStateBar.css";

const STATE_LABELS = {
    TODO: "To Do",
    IN_PROGRESS: "In Progress",
    TO_BE_REVIEWED: "To Be Reviewed",
    DONE: "Done",
};
const STATE_OPTIONS = ["TODO", "IN_PROGRESS", "TO_BE_REVIEWED", "DONE"];

export default function TaskStateBar({
    canChange, editing, editState, taskState,
    onEditStateChange, onStateChange,
}) {
    return (
        <div className="task-state-bar">
            {canChange ? (
                <select
                    className="task-state-select"
                    value={editing ? editState : taskState}
                    onChange={(e) => {
                        if (editing) onEditStateChange(e.target.value);
                        else onStateChange(e.target.value);
                    }}
                >
                    {STATE_OPTIONS.map((s) => (
                        <option key={s} value={s}>{STATE_LABELS[s]}</option>
                    ))}
                </select>
            ) : (
                <span className="task-state-badge">
                    {STATE_LABELS[taskState] || taskState}
                </span>
            )}
        </div>
    );
}
