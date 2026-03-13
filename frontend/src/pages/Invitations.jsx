import { useState, useEffect, useContext, useCallback, useRef } from "react";
import { FiRefreshCw } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { GroupContext } from "@context/GroupContext";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPatch } from "@assets/js/apiClient.js";
import Spinner from "@components/Spinner";
import InvitationCard from "@components/invitations/InvitationCard";
import InvitationsSentTable from "@components/invitations/InvitationsSentTable";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Invitations.css";

export default function Invitations() {
    const { user, setUser } = useContext(AuthContext);
    const { groups, reloadGroups } = useContext(GroupContext);
    const showToast = useToast();

    usePageTitle("Invitations");

    const [received, setReceived] = useState([]);
    const [sent, setSent] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [lastSeenBefore, setLastSeenBefore] = useState(null);

    const userRef = useRef(user);
    userRef.current = user;

    const fetchInvitations = useCallback(async (isRefresh = false) => {
        if (isRefresh) setRefreshing(true); else setLoading(true);
        try {
            const lsBefore = userRef.current?.lastSeenInvites
                ? new Date(userRef.current.lastSeenInvites)
                : null;
            setLastSeenBefore(lsBefore);

            const [inv, sentInv] = await Promise.all([
                apiGet("/api/group-invitations/me"),
                apiGet("/api/group-invitations/sent"),
            ]);
            setReceived(Array.isArray(inv) ? inv : []);
            setSent(Array.isArray(sentInv) ? sentInv : []);

            setUser((prev) =>
                prev ? { ...prev, lastSeenInvites: new Date().toISOString() } : prev
            );
        } catch (err) {
            showToast(err?.message || "Failed to load invitations");
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [setUser, showToast]);

    useEffect(() => { fetchInvitations(); }, []);

    useEffect(() => {
        const handler = () => fetchInvitations(true);
        window.addEventListener("invites-changed", handler);
        return () => window.removeEventListener("invites-changed", handler);
    }, [fetchInvitations]);

    async function handleRespond(invId, status) {
        try {
            const updated = await apiPatch(
                `/api/group-invitations/${invId}/status?status=${status}`
            );
            setReceived((prev) =>
                prev.map((inv) => (inv.id === invId ? updated : inv))
            );
            if (status === "ACCEPTED") reloadGroups();
        } catch (err) {
            showToast(err?.message || "Failed to respond to invitation", "error");

            setTimeout(() => fetchInvitations(true), 600);
        }
    }

    function isUnread(inv) {
        if (!lastSeenBefore || !inv.createdAt) return false;
        return new Date(inv.createdAt) > lastSeenBefore;
    }

    if (loading) return <Spinner />;

    const pendingReceived = received.filter(
        (inv) => inv.invitationStatus === "PENDING"
    );
    const resolvedReceived = received.filter(
        (inv) => inv.invitationStatus !== "PENDING"
    );

    return (
        <div className="invitations-page">
            <div className="invitations-header-row">
                <h1 className="invitations-heading">Invitations</h1>
                <button
                    className="btn-secondary btn-sm invitations-refresh-btn"
                    onClick={() => fetchInvitations(true)}
                    disabled={refreshing}
                    title="Refresh invitations"
                >
                    <FiRefreshCw size={14} className={refreshing ? "spin" : ""} />
                    {refreshing ? "Refreshing…" : "Refresh"}
                </button>
            </div>

            <div className="invitations-group-count">
                Groups: {groups.length}/3
            </div>

            <section className="invitations-section">
                <h2 className="invitations-section-title">Received</h2>

                {pendingReceived.length === 0 && resolvedReceived.length === 0 ? (
                    <p className="invitations-empty">No invitations yet.</p>
                ) : (
                    <>
                        {pendingReceived.map((inv) => (
                            <InvitationCard
                                key={inv.id}
                                inv={inv}
                                isUnread={isUnread(inv)}
                                isPending
                                onRespond={handleRespond}
                            />
                        ))}

                        {resolvedReceived.length > 0 && (
                            <details className="invitations-resolved-toggle">
                                <summary>
                                    Past invitations ({resolvedReceived.length})
                                </summary>
                                {resolvedReceived.map((inv) => (
                                    <InvitationCard
                                        key={inv.id}
                                        inv={inv}
                                        isUnread={false}
                                        isPending={false}
                                    />
                                ))}
                            </details>
                        )}
                    </>
                )}
            </section>

            <section className="invitations-section">
                <h2 className="invitations-section-title">Your pending invites</h2>
                <InvitationsSentTable sent={sent} />
            </section>
        </div>
    );
}
