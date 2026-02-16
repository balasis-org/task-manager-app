import { useState, useEffect } from "react";
import { apiGet } from "@assets/js/apiClient";
import "@styles/popups/Popup.css";

export default function GroupEventsPopup({ groupId, onClose }) {
    const [events, setEvents] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadEvents(page);
    }, [page, groupId]);

    async function loadEvents(p) {
        setLoading(true);
        try {
            const data = await apiGet(
                `/api/groups/${groupId}/events?page=${p}&size=10`
            );
            setEvents(data?.content ?? []);
            setTotalPages(data?.totalPages ?? 0);
        } catch {
            setEvents([]);
        } finally {
            setLoading(false);
        }
    }

    function formatDate(iso) {
        if (!iso) return "";
        return new Date(iso).toLocaleString();
    }

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div
                className="popup-card popup-card-wide"
                onClick={(e) => e.stopPropagation()}
            >
                <h2>Group Events</h2>

                {loading ? (
                    <p>Loading…</p>
                ) : events.length === 0 ? (
                    <p className="muted">No events yet.</p>
                ) : (
                    <ul className="popup-event-list">
                        {events.map((ev) => (
                            <li key={ev.id}>
                                <span>{ev.description}</span>
                                <span className="popup-event-date">
                                    {formatDate(ev.createdAt)}
                                </span>
                            </li>
                        ))}
                    </ul>
                )}

                {/* Pagination */}
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
                    <button className="btn-secondary" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}
