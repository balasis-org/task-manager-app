import { useEffect, useState } from "react";
import { FiX } from "react-icons/fi";
import { apiGet } from "@assets/js/apiClient.js";
import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/popups/DefaultImagePicker.css";

// grid popup for picking from server-hosted default images.
// type="user" or "group" — fetches the list from /api/users/me/default-images?type=.
// images live in the "default-images/" blob container prefix.
export default function DefaultImagePicker({ type, onPick, onClose }) {
    const blobUrl = useBlobUrl();
    const [images, setImages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let alive = true;
        setLoading(true);
        setError(null);
        apiGet(`/api/users/me/default-images?type=${type}`)
            .then((data) => {
                if (alive) setImages(data);
            })
            .catch(() => {
                if (alive) setError("Could not load default images.");
            })
            .finally(() => {
                if (alive) setLoading(false);
            });
        return () => { alive = false; };
    }, [type]);

    return (
        <div className="dflt-picker-overlay" onClick={onClose}>
            <div className="dflt-picker-panel" onClick={(e) => e.stopPropagation()}>
                <div className="dflt-picker-header">
                    <span className="dflt-picker-title">Pick a default image</span>
                    <button className="dflt-picker-close" onClick={onClose} title="Close">
                        <FiX size={16} />
                    </button>
                </div>

                {loading && <p className="dflt-picker-hint">Loading…</p>}
                {error && <p className="dflt-picker-hint dflt-picker-error">{error}</p>}

                {!loading && !error && images.length === 0 && (
                    <p className="dflt-picker-hint">No defaults available.</p>
                )}

                {!loading && !error && images.length > 0 && (
                    <div className="dflt-picker-grid">
                        {images.map((fileName) => {
                            const fullPath = "default-images/" + fileName;
                            return (
                                <button
                                    key={fileName}
                                    className="dflt-picker-item"
                                    onClick={() => { onPick(fileName); onClose(); }}
                                    title={fileName}
                                >
                                    <img
                                        src={blobUrl(fullPath)}
                                        alt={fileName}
                                        className="dflt-picker-img"
                                    />
                                </button>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
}
