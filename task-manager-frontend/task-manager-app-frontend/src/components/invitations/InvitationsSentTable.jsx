import "@styles/invitations/InvitationsSentTable.css";

export default function InvitationsSentTable({ sent }) {
    if (!sent?.length) {
        return <p className="invitations-empty">No pending invites.</p>;
    }

    return (
        <div className="invitations-table-wrapper">
            <table className="invitations-table">
                <thead>
                    <tr>
                        <th>For Group</th>
                        <th>To</th>
                        <th>Comment</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    {sent.map((inv) => (
                        <tr key={inv.id}>
                            <td>
                                <span className="sent-group-name" title={inv.groupName}>
                                    {inv.groupName}
                                </span>
                            </td>
                            <td>{inv.user?.name || inv.user?.email || "—"}</td>
                            <td>
                                <span
                                    className="sent-comment-cell"
                                    title={inv.comment || ""}
                                >
                                    {inv.comment
                                        ? inv.comment.length > 30
                                            ? inv.comment.slice(0, 30) + "…"
                                            : inv.comment
                                        : "—"}
                                </span>
                            </td>
                            <td>
                                <span className={`invitation-status ${inv.invitationStatus?.toLowerCase()}`}>
                                    {inv.invitationStatus}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
