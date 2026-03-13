import "@styles/dashboard/DashboardColumnHeaders.css";

const COL_NAMES   = ["Title", "Creator", "Priority", "Due date", "\u{1F512}", "\uD83D\uDCAC"];
const COL_CLASSES = ["col-title", "col-creator", "col-priority", "col-due", "col-access", "col-comments"];

export default function DashboardColumnHeaders({
    visCols,
    gridTemplate,
    showDeleteColumn,
}) {
    return (
        <div
            className="task-col-header"
            style={{ gridTemplateColumns: gridTemplate }}
        >
            {visCols.map((ci) => (
                <span key={ci} className={`col-header-cell ${COL_CLASSES[ci]}`}>
                    {ci === 2 ? (
                        <>
                            <span className="col-text-full">Priority</span>
                            <span className="col-text-short">PR</span>
                        </>
                    ) : COL_NAMES[ci]}
                </span>
            ))}
            {showDeleteColumn && (
                <span className="col-header-cell col-delete" />
            )}
        </div>
    );
}
