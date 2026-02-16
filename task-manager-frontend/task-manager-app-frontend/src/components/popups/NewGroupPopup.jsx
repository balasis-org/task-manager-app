import { useState } from "react";
import { apiPost, apiMultipart } from "@assets/js/apiClient";
import "@styles/popups/Popup.css";

export default function NewGroupPopup({ onClose, onCreated }) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [coverImage, setCoverImage] = useState(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!title.trim()) {
            setError("Title is required.");
            return;
        }

        setBusy(true);
        setError("");
        try {
            // Create group
            const group = await apiPost("/api/groups", {
                name: title.trim(),
                description: description.trim(),
                Announcement: "",
                allowEmailNotification: true,
            });

            // If cover image selected, upload it
            if (coverImage && group?.id) {
                const fd = new FormData();
                fd.append("file", coverImage);
                try {
                    const updated = await apiMultipart(
                        `/api/groups/${group.id}/image`,
                        fd
                    );
                    onCreated(updated);
                    return;
                } catch {
                    // Image upload failed but group was created
                }
            }

            onCreated(group);
        } catch {
            setError("Failed to create group.");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card" onClick={(e) => e.stopPropagation()}>
                <h2>New group</h2>

                {error && <div className="popup-error">{error}</div>}

                <form onSubmit={handleSubmit} className="popup-form">
                    <label>
                        Title
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            maxLength={100}
                            required
                        />
                    </label>

                    <label>
                        Description
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                        />
                    </label>

                    <label>
                        Cover Image
                        <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => setCoverImage(e.target.files?.[0] || null)}
                        />
                    </label>

                    <div className="popup-actions">
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={onClose}
                            disabled={busy}
                        >
                            Cancel
                        </button>
                        <button type="submit" className="btn-primary" disabled={busy}>
                            {busy ? "Creating…" : "Confirm"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
