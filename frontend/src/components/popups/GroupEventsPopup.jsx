import { useState, useEffect } from "react";
import { apiGet, apiDelete } from "@assets/js/apiClient";
import { FiTrash2, FiAlertTriangle } from "react-icons/fi";
import "@styles/popups/Popup.css";
import "@styles/popups/GroupEventsPopup.css";

// paginated audit log for a group — shows membership changes, task state
// changes, file uploads, etc. events before seenThreshold are dimmed.
// leader can bulk-clear all events.
export default function GroupEventsPopup({ groupId, onClose, lastSeenGroupEvents, isLeader }) {
    const [events, setEvents] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [initialLoad, setInitialLoad] = useState(true);
    const [confirmClear, setConfirmClear] = useState(false);
    const [clearing, setClearing] = useState(false);

    const [seenThreshold] = useState(() => lastSeenGroupEvents || null);

    useEffect(() => {
        loadEvents(page);
    }, [page, groupId]);

    async function loadEvents(p) {
        setLoading(true);
        try {
            const data = await apiGet(
                `/api/groups/${groupId}/events?page=${p}&size=10&sort=createdAt,desc`
            );
            setEvents(data?.content ?? []);
            setTotalPages(data?.totalPages ?? 0);
        } catch {
            setEvents([]);
        } finally {
            setLoading(false);
            setInitialLoad(false);
        }
    }

    function formatDate(iso) {
        if (!iso) return "";
        return new Date(iso).toLocaleString();
    }

    async function handleClearAll() {
        setClearing(true);
        try {
            await apiDelete(`/api/groups/${groupId}/events`);
            setEvents([]);
            setTotalPages(0);
            setPage(0);
            setConfirmClear(false);
        } catch {
            /* swallow - toast could be added later */
        } finally {
            setClearing(false);
        }
    }

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div
                className="popup-card popup-card-wide"
                style={{ minHeight: "22em" }}
                onClick={(e) => e.stopPropagation()}
            >
                <h2>Group Events</h2>

                <div className="popup-events-body">
                    {initialLoad ? (
                        <p style={{ textAlign: "center", paddingTop: "4em", color: "var(--text-muted)" }}>Loading…</p>
                    ) : events.length === 0 ? (
                        <p className="muted">No events yet.</p>
                    ) : (
                        <ul className={`popup-event-list${loading ? " popup-events-loading" : ""}`}>
                            {events.map((ev) => {
                                const isNew = seenThreshold
                                    ? new Date(ev.createdAt) > new Date(seenThreshold)
                                    : true;
                                return (
                                    <li key={ev.id} className={isNew ? "popup-event-new" : ""}>
                                        {isNew && <span className="popup-event-new-badge">NEW</span>}
                                        <span>{ev.description}</span>
                                        <span className="popup-event-date">
                                            {formatDate(ev.createdAt)}
                                        </span>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                {totalPages > 1 && (
                    <div className="popup-pagination">
                        <button
                            disabled={page <= 0}
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                        >
                            ←
                        </button>
                        <span>
                            {page + 1} / {totalPages}
                        </span>
                        <button
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage((p) => p + 1)}
                        >
                            →
                        </button>
                    </div>
                )}

                <div className="popup-actions">
                    {isLeader && events.length > 0 && !confirmClear && (
                        <button
                            className="popup-clear-btn"
                            onClick={() => setConfirmClear(true)}
                        >
                            <FiTrash2 size={13} /> Clear all
                        </button>
                    )}
                    {confirmClear && (
                        <div className="popup-confirm-box">
                            <FiAlertTriangle size={15} className="popup-icon-warn" />
                            <span>Delete all events?</span>
                            <button
                                className="popup-confirm-yes"
                                onClick={handleClearAll}
                                disabled={clearing}
                            >
                                {clearing ? "Deleting…" : "Confirm"}
                            </button>
                            <button
                                className="popup-confirm-no"
                                onClick={() => setConfirmClear(false)}
                            >
                                Cancel
                            </button>
                        </div>
                    )}
                    <button className="btn-secondary" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}
