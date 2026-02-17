import { useState, useEffect, useContext } from "react";
import { FiCheck, FiX, FiTrash2 } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPatch, apiDelete } from "@assets/js/apiClient.js";
import Spinner from "@components/Spinner";
import "@styles/pages/Invitations.css";

function formatDate(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
    });
}

export default function Invitations() {
    const { user, setUser } = useContext(AuthContext);
    const showToast = useToast();

    const [received, setReceived] = useState([]);
    const [sent, setSent] = useState([]);
    const [loading, setLoading] = useState(true);
    const [lastSeenBefore, setLastSeenBefore] = useState(null);

    // ── Fetch both lists on mount ──
    useEffect(() => {
        (async () => {
            setLoading(true);
            try {
                // Grab profile BEFORE fetching /me invitations (which updates lastSeenInvites)
                const profile = await apiGet("/api/users/me");
                const lsBefore = profile?.lastSeenInvites
                    ? new Date(profile.lastSeenInvites)
                    : null;
                setLastSeenBefore(lsBefore);

                const [inv, sentInv] = await Promise.all([
                    apiGet("/api/group-invitations/me"),
                    apiGet("/api/group-invitations/sent"),
                ]);
                setReceived(Array.isArray(inv) ? inv : []);
                setSent(Array.isArray(sentInv) ? sentInv : []);

                // After fetching, the backend marked invites as seen.
                // Update the user context so sidebar badge clears.
                setUser((prev) =>
                    prev ? { ...prev, lastSeenInvites: new Date().toISOString() } : prev
                );
            } catch {
                showToast("Failed to load invitations");
            } finally {
                setLoading(false);
            }
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // ── Respond to an invitation (ACCEPTED / DECLINED) ──
    async function handleRespond(invId, status) {
        try {
            const updated = await apiPatch(
                `/api/group-invitations/${invId}/status?status=${status}`
            );
            setReceived((prev) =>
                prev.map((inv) => (inv.id === invId ? updated : inv))
            );
        } catch {
            showToast("Failed to respond to invitation");
        }
    }

    // ── Cancel a sent invitation ──
    async function handleCancel(invId) {
        try {
            await apiDelete(`/api/group-invitations/${invId}`);
            setSent((prev) => prev.filter((inv) => inv.id !== invId));
        } catch {
            showToast("Failed to cancel invitation");
        }
    }

    function isUnread(inv) {
        if (!lastSeenBefore || !inv.createdAt) return false;
        return new Date(inv.createdAt) > lastSeenBefore;
    }

    if (loading) return <Spinner />;

    // Only show PENDING received invitations that haven't been resolved
    const pendingReceived = received.filter(
        (inv) => inv.invitationStatus === "PENDING"
    );
    const resolvedReceived = received.filter(
        (inv) => inv.invitationStatus !== "PENDING"
    );

    return (
        <div className="invitations-page">
            <h1 className="invitations-heading">Invitations</h1>

            {/* ── Received invitations ── */}
            <section className="invitations-section">
                <h2 className="invitations-section-title">Received</h2>

                {pendingReceived.length === 0 && resolvedReceived.length === 0 ? (
                    <p className="invitations-empty">No invitations yet.</p>
                ) : (
                    <>
                        {pendingReceived.map((inv) => (
                            <div
                                key={inv.id}
                                className={`invitation-card${isUnread(inv) ? " unread" : ""}`}
                            >
                                <div className="invitation-card-top">
                                    <span className="invitation-group" title={inv.groupName}>
                                        {inv.groupName}
                                    </span>
                                    <span className="invitation-from">
                                        Inviter: {inv.invitedBy?.name || inv.invitedBy?.email || "—"}
                                    </span>
                                </div>
                                {inv.comment && (
                                    <p className="invitation-comment">
                                        Comment: {inv.comment}
                                    </p>
                                )}
                                <div className="invitation-card-bottom">
                                    <span className="invitation-date">
                                        Date: {formatDate(inv.createdAt)}
                                    </span>
                                    <div className="invitation-actions">
                                        <button
                                            className="btn-secondary btn-sm"
                                            onClick={() => handleRespond(inv.id, "DECLINED")}
                                        >
                                            Reject
                                        </button>
                                        <button
                                            className="btn-primary btn-sm"
                                            onClick={() => handleRespond(inv.id, "ACCEPTED")}
                                        >
                                            Accept
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}

                        {resolvedReceived.length > 0 && (
                            <details className="invitations-resolved-toggle">
                                <summary>
                                    Past invitations ({resolvedReceived.length})
                                </summary>
                                {resolvedReceived.map((inv) => (
                                    <div key={inv.id} className="invitation-card resolved">
                                        <div className="invitation-card-top">
                                            <span className="invitation-group" title={inv.groupName}>
                                                {inv.groupName}
                                            </span>
                                            <span className={`invitation-status ${inv.invitationStatus?.toLowerCase()}`}>
                                                {inv.invitationStatus}
                                            </span>
                                        </div>
                                        <div className="invitation-card-bottom">
                                            <span className="invitation-date">
                                                {formatDate(inv.createdAt)}
                                            </span>
                                            <span className="invitation-from">
                                                From: {inv.invitedBy?.name || inv.invitedBy?.email || "—"}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </details>
                        )}
                    </>
                )}
            </section>

            {/* ── Sent / pending invitations ── */}
            <section className="invitations-section">
                <h2 className="invitations-section-title">Your pending invites</h2>


                {sent.length === 0 ? (
                    <p className="invitations-empty">No pending invites.</p>
                ) : (
                    <div className="invitations-table-wrapper">
                        <table className="invitations-table">
                            <thead>
                                <tr>
                                    <th>For Group</th>
                                    <th>To</th>
                                    <th>Comment</th>
                                    <th>Status</th>
                                    <th></th>
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
                                        <td>
                                            {inv.invitationStatus === "PENDING" && (
                                                <button
                                                    className="btn-abort"
                                                    onClick={() => handleCancel(inv.id)}
                                                    title="Cancel invitation"
                                                >
                                                    Abort
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    );
}