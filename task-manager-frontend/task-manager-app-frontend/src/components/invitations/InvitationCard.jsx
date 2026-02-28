import { formatDate } from "@assets/js/formatDate";
import "@styles/invitations/InvitationCard.css";

// isPending controls the two layouts: action buttons vs. status badge
export default function InvitationCard({ inv, isUnread, isPending, onRespond }) {
    return (
        <div
            className={`invitation-card${isUnread ? " unread" : ""}${!isPending ? " resolved" : ""}`}
        >
            <div className="invitation-card-top">
                <span className="invitation-group" title={inv.groupName}>
                    {inv.groupName}
                </span>
                {isPending ? (
                    <span className="invitation-from">
                        Inviter: {inv.invitedBy?.name || inv.invitedBy?.email || "—"}
                    </span>
                ) : (
                    <span className={`invitation-status ${inv.invitationStatus?.toLowerCase()}`}>
                        {inv.invitationStatus}
                    </span>
                )}
            </div>
            {isPending && inv.comment && (
                <p className="invitation-comment">
                    Comment: {inv.comment}
                </p>
            )}
            <div className="invitation-card-bottom">
                <span className="invitation-date">
                    {isPending ? "Date: " : ""}{formatDate(inv.createdAt)}
                </span>
                {isPending ? (
                    <div className="invitation-actions">
                        <button
                            className="btn-secondary btn-sm"
                            onClick={() => onRespond(inv.id, "DECLINED")}
                        >
                            Reject
                        </button>
                        <button
                            className="btn-primary btn-sm"
                            onClick={() => onRespond(inv.id, "ACCEPTED")}
                        >
                            Accept
                        </button>
                    </div>
                ) : (
                    <span className="invitation-from">
                        From: {inv.invitedBy?.name || inv.invitedBy?.email || "—"}
                    </span>
                )}
            </div>
        </div>
    );
}
