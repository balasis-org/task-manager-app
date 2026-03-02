import { FiPlus } from "react-icons/fi";
import TaskTable from "@components/dashboard/TaskTable";
import "@styles/dashboard/DashboardTaskSection.css";

export default function DashboardTaskSection({
    state,
    label,
    collapsed,
    tasks,
    onToggle,
    canManageTasks,
    showDeleteColumn,
    onOpenNewTask,
    groupId,
    colWidths,
    visCols,
    onDeleted,
}) {
    return (
        <div className="task-section" data-state={state}>
            <div
                className="task-section-header"
                onClick={onToggle}
            >
                <span className="task-section-arrow">
                    {collapsed ? "▶" : "▼"}
                </span>
                <span className="task-section-title">
                    {label}
                    {canManageTasks && (
                        <button
                            className="task-section-add"
                            title="Create task"
                            onClick={(e) => onOpenNewTask(e, state)}
                        >
                            <FiPlus size={12} />
                        </button>
                    )}
                </span>
                <span className="task-section-count">
                    ({tasks.length})
                </span>
            </div>
            {!collapsed && (
                <TaskTable
                    tasks={tasks}
                    groupId={groupId}
                    colWidths={colWidths}
                    visCols={visCols}
                    canManageTasks={showDeleteColumn}
                    onDeleted={onDeleted}
                />
            )}
        </div>
    );
}
