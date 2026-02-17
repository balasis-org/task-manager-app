import { useState } from "react";
import { apiPatch, apiMultipart } from "@assets/js/apiClient";
import { LIMITS } from "@assets/js/inputValidation";
import "@styles/popups/Popup.css";

export default function GroupSettingsPopup({ group, onClose, onUpdated }) {
    const [description, setDescription] = useState(group.description || "");
    const [emailNotif, setEmailNotif] = useState(group.allowEmailNotification ?? true);
    const [coverImage, setCoverImage] = useState(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        setBusy(true);
        setError("");
        try {
            const updated = await apiPatch(`/api/groups/${group.id}`, {
                description: description.trim(),
                allowEmailNotification: emailNotif,
            });

            // Upload image if selected
            if (coverImage) {
                const fd = new FormData();
                fd.append("file", coverImage);
                try {
                    const withImg = await apiMultipart(
                        `/api/groups/${group.id}/image`,
                        fd
                    );
                    onUpdated(withImg);
                    return;
                } catch {
                    // image failed but patch succeeded
                }
            }

            onUpdated(updated);
        } catch {
            setError("Failed to update group.");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card" onClick={(e) => e.stopPropagation()}>
                <h2 title={group.name} className="popup-heading-ellipsis">{group.name} Settings</h2>

                {error && <div className="popup-error">{error}</div>}

                <form onSubmit={handleSubmit} className="popup-form">
                    <label className="popup-row">
                        Change group image
                        <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => setCoverImage(e.target.files?.[0] || null)}
                        />
                    </label>

                    <label>
                        Email notifications
                        <select
                            value={emailNotif ? "on" : "off"}
                            onChange={(e) => setEmailNotif(e.target.value === "on")}
                        >
                            <option value="on">on</option>
                            <option value="off">off</option>
                        </select>
                    </label>

                    <label>
                        Description
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            maxLength={LIMITS.GROUP_DESCRIPTION}
                        />
                        <span className="char-count">{description.length}/{LIMITS.GROUP_DESCRIPTION}</span>
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
                            {busy ? "Saving…" : "Confirm"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
