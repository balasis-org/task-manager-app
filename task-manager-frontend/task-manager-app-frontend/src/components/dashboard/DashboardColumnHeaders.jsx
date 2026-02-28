import "@styles/dashboard/DashboardColumnHeaders.css";

const COL_NAMES   = ["Title", "Creator", "Priority", "Due date", "Accessible", "\uD83D\uDCAC"];
const COL_CLASSES = ["col-title", "col-creator", "col-priority", "col-due", "col-access", "col-comments"];

export default function DashboardColumnHeaders({
    visCols,
    gridTemplate,
    showDeleteColumn,
    onTitleHandleDown,
    onPointerDown,
}) {
    return (
        <div
            className="task-col-header"
            style={{ gridTemplateColumns: gridTemplate }}
        >
            {visCols.map((ci) => (
                <span key={ci} className={`col-header-cell ${COL_CLASSES[ci]}`}>
                    {COL_NAMES[ci]}
                    {/* Title → next-col resize handle */}
                    {ci === 0 && visCols.length > 1 && (
                        <span
                            className="col-resize-handle"
                            onPointerDown={(e) => onTitleHandleDown(visCols[1], e)}
                        />
                    )}
                    {/* Mid-column resize handle */}
                    {ci !== 0 && ci !== visCols[visCols.length - 1] && (
                        <span
                            className="col-resize-handle"
                            onPointerDown={(e) => onPointerDown(ci, e)}
                        />
                    )}
                </span>
            ))}
            {showDeleteColumn && (
                <span className="col-header-cell col-delete" />
            )}
        </div>
    );
}
