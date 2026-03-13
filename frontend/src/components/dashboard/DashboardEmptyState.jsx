import NewGroupPopup from "@components/popups/NewGroupPopup";
import "@styles/dashboard/DashboardEmptyState.css";

const DashboardEmptyState = ({ showNewGroup, onOpenNewGroup, onCloseNewGroup, onGroupCreated, user }) => (
    <div className="dashboard-empty">
        <h2>Welcome!</h2>
        <p>You don't have any groups yet. Create one to get started.</p>
        <button
            className="btn-primary"
            onClick={onOpenNewGroup}
        >
            + Create a group
        </button>
        {showNewGroup && (
            <NewGroupPopup
                onClose={onCloseNewGroup}
                onCreated={onGroupCreated}
                user={user}
            />
        )}
    </div>
);

export default DashboardEmptyState;
