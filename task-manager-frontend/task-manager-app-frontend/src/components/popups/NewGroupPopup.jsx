import { useEffect, useRef, useState } from "react";
import { apiPost, apiMultipart } from "@assets/js/apiClient";
import { LIMITS } from "@assets/js/inputValidation";
import { isImageTooLarge } from "@assets/js/fileUtils";
import { FiUsers, FiImage } from "react-icons/fi";
import "@styles/popups/Popup.css";

export default function NewGroupPopup({ onClose, onCreated }) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [coverImage, setCoverImage] = useState(null);
    const [coverPreview, setCoverPreview] = useState(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [dragOver, setDragOver] = useState(false);
    const fileInputRef = useRef(null);

    const updateCover = (file) => {
        setCoverImage(file);
        setCoverPreview((prevUrl) => {
            if (prevUrl) URL.revokeObjectURL(prevUrl);
            return file ? URL.createObjectURL(file) : null;
        });
    };

    const handleCoverChange = (e) => {
        const file = e.target.files?.[0] || null;
        updateCover(file);
        e.target.value = "";
    };

    const clearCover = () => updateCover(null);

    const openFileDialog = () => fileInputRef.current?.click();

    const handleDropzoneKeyDown = (e) => {
        if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            openFileDialog();
        }
    };

    const handleDragOver = (e) => { e.preventDefault(); setDragOver(true); };
    const handleDragLeave = () => setDragOver(false);
    const handleDrop = (e) => {
        e.preventDefault();
        setDragOver(false);
        const file = e.dataTransfer.files?.[0];
        if (!file) return;
        if (!file.type.startsWith("image/")) { setError("Only image files are allowed."); return; }
        if (isImageTooLarge(file)) { setError(`Image must be under ${LIMITS.MAX_IMAGE_SIZE_MB} MB.`); return; }
        setError("");
        updateCover(file);
    };

    useEffect(() => () => {
        if (coverPreview) URL.revokeObjectURL(coverPreview);
    }, [coverPreview]);

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
        } catch (err) {
            setError(err?.message || "Failed to create group.");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card popup-card-wide new-group-popup-card" onClick={(e) => e.stopPropagation()}>
                <div className="new-group-header">
                    <div className="new-group-icon">
                        <FiUsers size={22} />
                    </div>
                    <div>
                        <h2>New group</h2>
                        <p>Give it a name, context, and optional banner to welcome members.</p>
                    </div>
                </div>

                {error && <div className="popup-error">{error}</div>}

                <form onSubmit={handleSubmit} className="popup-form new-group-form">
                    <div className="popup-field">
                        <label htmlFor="group-title" className="popup-label">
                            Group name
                            <span className="label-required">required</span>
                        </label>
                        <input
                            id="group-title"
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            maxLength={LIMITS.GROUP_NAME}
                            placeholder="e.g. Product Launch Crew"
                            required
                        />
                    </div>

                    <div className="popup-field">
                        <label htmlFor="group-description" className="popup-label">
                            Description
                            <span className="label-optional">optional</span>
                        </label>
                        <textarea
                            id="group-description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            maxLength={LIMITS.GROUP_DESCRIPTION}
                            placeholder="Share what this group is about or what you will collaborate on."
                        />
                        <span className="char-count">{description.length}/{LIMITS.GROUP_DESCRIPTION}</span>
                        <p className="popup-field-hint">You can always edit this later from group settings.</p>
                    </div>

                    <div className="popup-field">
                        <label className="popup-label">
                            Cover image
                            <span className="label-optional">optional</span>
                        </label>
                        <div
                            className={`new-group-dropzone ${coverPreview ? "has-preview" : ""}${dragOver ? " drag-over" : ""}`}
                            role="button"
                            tabIndex={0}
                            onClick={openFileDialog}
                            onKeyDown={handleDropzoneKeyDown}
                            onDragOver={handleDragOver}
                            onDragLeave={handleDragLeave}
                            onDrop={handleDrop}
                        >
                            {coverPreview ? (
                                <>
                                    <img src={coverPreview} alt="Cover preview" />
                                    <span className="new-group-dropzone-hint">Click to replace</span>
                                </>
                            ) : (
                                <>
                                    <FiImage size={28} />
                                    <strong>Upload a banner</strong>
                                    <span>PNG or JPG, up to 5 MB.</span>
                                </>
                            )}
                        </div>
                        {coverImage && (
                            <div className="new-group-file-row">
                                <span>{coverImage.name}</span>
                                <button type="button" className="link-btn" onClick={clearCover}>
                                    Remove
                                </button>
                            </div>
                        )}
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/*"
                            hidden
                            onChange={handleCoverChange}
                        />
                    </div>

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
