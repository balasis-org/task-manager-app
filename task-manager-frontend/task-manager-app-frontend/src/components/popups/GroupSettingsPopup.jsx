import { useState, useRef } from "react";
import { FiEdit2, FiCheck, FiX, FiSearch, FiTrash2, FiShield, FiAlertTriangle } from "react-icons/fi";
import { apiPatch, apiDelete, apiMultipart } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { LIMITS } from "@assets/js/inputValidation";
import { isImageTooLarge } from "@assets/js/fileUtils";
import "@styles/popups/Popup.css";
import blobBase from "@blobBase";

export default function GroupSettingsPopup({ group, members, user, onClose, onUpdated, onDeleted }) {
    const showToast = useToast();

    /* description */
    const [editDesc, setEditDesc] = useState(false);
    const [description, setDescription] = useState(group.description || "");
    const [savingDesc, setSavingDesc] = useState(false);

    /* announcement */
    const [editAnn, setEditAnn] = useState(false);
    const [announcement, setAnnouncement] = useState(group.announcement || "");
    const [savingAnn, setSavingAnn] = useState(false);

    /* email notifs */
    const [editEmail, setEditEmail] = useState(false);
    const [emailNotif, setEmailNotif] = useState(group.allowEmailNotification ?? true);
    const [savingEmail, setSavingEmail] = useState(false);

    /* Image */
    const [coverImage, setCoverImage] = useState(null);
    const [uploadingImg, setUploadingImg] = useState(false);
    const [imgDragOver, setImgDragOver] = useState(false);
    const fileRef = useRef(null);

    /* Transfer leadership */
    const [showTransfer, setShowTransfer] = useState(false);
    const [transferSearch, setTransferSearch] = useState("");
    const [transferTarget, setTransferTarget] = useState(null);
    const [confirmTransfer, setConfirmTransfer] = useState(false);
    const [transferring, setTransferring] = useState(false);

    /* Delete group */
    const [confirmDelete, setConfirmDelete] = useState(false);
    const [deleting, setDeleting] = useState(false);

    // members excluding self (for transfer picker)
    const otherMembers = (members || []).filter(m => m.user?.id !== user?.id);
    const filteredTransfer = otherMembers.filter(m => {
        if (!transferSearch.trim()) return true;
        const q = transferSearch.toLowerCase();
        return (m.user?.name || "").toLowerCase().includes(q)
            || (m.user?.email || "").toLowerCase().includes(q);
    });

    // save helpers
    async function saveDescription() {
        setSavingDesc(true);
        try {
            const updated = await apiPatch(`/api/groups/${group.id}`, { description: description.trim() });
            onUpdated(updated);
            showToast("Description updated", "success");
            setEditDesc(false);
        } catch { showToast("Failed to update description"); }
        finally { setSavingDesc(false); }
    }

    async function saveAnnouncement() {
        setSavingAnn(true);
        try {
            const updated = await apiPatch(`/api/groups/${group.id}`, { announcement: announcement.trim() });
            onUpdated(updated);
            showToast("Announcement updated", "success");
            setEditAnn(false);
        } catch { showToast("Failed to update announcement"); }
        finally { setSavingAnn(false); }
    }

    async function saveEmailNotif() {
        setSavingEmail(true);
        try {
            const updated = await apiPatch(`/api/groups/${group.id}`, { allowEmailNotification: emailNotif });
            onUpdated(updated);
            showToast("Email setting updated", "success");
            setEditEmail(false);
        } catch { showToast("Failed to update email setting"); }
        finally { setSavingEmail(false); }
    }

    async function uploadImage() {
        if (!coverImage) return;
        setUploadingImg(true);
        try {
            const fd = new FormData();
            fd.append("file", coverImage);
            const updated = await apiMultipart(`/api/groups/${group.id}/image`, fd);
            onUpdated(updated);
            showToast("Group image updated", "success");
            setCoverImage(null);
        } catch { showToast("Failed to upload image"); }
        finally { setUploadingImg(false); }
    }

    /* Transfer leadership */
    async function handleTransfer() {
        if (!transferTarget) return;
        setTransferring(true);
        try {
            await apiPatch(
                `/api/groups/${group.id}/groupMembership/${transferTarget.id}/role?role=GROUP_LEADER`
            );
            showToast("Leadership transferred!", "success");
            onClose();
        } catch { showToast("Failed to transfer leadership"); }
        finally { setTransferring(false); }
    }

    /* Delete group */
    async function handleDelete() {
        setDeleting(true);
        try {
            await apiDelete(`/api/groups/${group.id}`);
            showToast("Group deleted", "success");
            if (onDeleted) onDeleted();
        } catch { showToast("Failed to delete group"); }
        finally { setDeleting(false); }
    }

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card popup-card-wide gs-popup" onClick={e => e.stopPropagation()}>
                <h2 title={group.name} className="popup-heading-ellipsis">
                    {group.name} — Settings
                </h2>

                {/* Description */}
                <section className="gs-section">
                    <div className="gs-section-header">
                        <span className="gs-section-label">Description</span>
                        {!editDesc && (
                            <button className="gs-edit-btn" onClick={() => setEditDesc(true)} title="Edit">
                                <FiEdit2 size={13} />
                            </button>
                        )}
                    </div>
                    {editDesc ? (
                        <div className="gs-field-edit">
                            <textarea
                                value={description}
                                onChange={e => setDescription(e.target.value)}
                                rows={3}
                                maxLength={LIMITS.GROUP_DESCRIPTION}
                            />
                            <span className="char-count">{description.length}/{LIMITS.GROUP_DESCRIPTION}</span>
                            <div className="gs-field-actions">
                                <button className="gs-save-btn" onClick={saveDescription} disabled={savingDesc}>
                                    <FiCheck size={13} /> {savingDesc ? "Saving…" : "Save"}
                                </button>
                                <button className="gs-cancel-btn" onClick={() => { setEditDesc(false); setDescription(group.description || ""); }}>
                                    <FiX size={13} /> Cancel
                                </button>
                            </div>
                        </div>
                    ) : (
                        <p className="gs-field-value">{group.description || <em className="muted">No description</em>}</p>
                    )}
                </section>

                {/* Announcement */}
                <section className="gs-section">
                    <div className="gs-section-header">
                        <span className="gs-section-label">Announcement</span>
                        {!editAnn && (
                            <button className="gs-edit-btn" onClick={() => setEditAnn(true)} title="Edit">
                                <FiEdit2 size={13} />
                            </button>
                        )}
                    </div>
                    {editAnn ? (
                        <div className="gs-field-edit">
                            <textarea
                                value={announcement}
                                onChange={e => setAnnouncement(e.target.value)}
                                rows={2}
                                maxLength={LIMITS.GROUP_ANNOUNCEMENT}
                            />
                            <span className="char-count">{announcement.length}/{LIMITS.GROUP_ANNOUNCEMENT}</span>
                            <div className="gs-field-actions">
                                <button className="gs-save-btn" onClick={saveAnnouncement} disabled={savingAnn}>
                                    <FiCheck size={13} /> {savingAnn ? "Saving…" : "Save"}
                                </button>
                                <button className="gs-cancel-btn" onClick={() => { setEditAnn(false); setAnnouncement(group.announcement || ""); }}>
                                    <FiX size={13} /> Cancel
                                </button>
                            </div>
                        </div>
                    ) : (
                        <p className="gs-field-value">{group.announcement || <em className="muted">No announcement</em>}</p>
                    )}
                </section>

                {/* Email notifications */}
                <section className="gs-section">
                    <div className="gs-section-header">
                        <span className="gs-section-label">Email notifications</span>
                        {!editEmail && (
                            <button className="gs-edit-btn" onClick={() => setEditEmail(true)} title="Edit">
                                <FiEdit2 size={13} />
                            </button>
                        )}
                    </div>
                    {editEmail ? (
                        <div className="gs-field-edit">
                            <select
                                value={emailNotif ? "on" : "off"}
                                onChange={e => setEmailNotif(e.target.value === "on")}
                            >
                                <option value="on">On</option>
                                <option value="off">Off</option>
                            </select>
                            <div className="gs-field-actions">
                                <button className="gs-save-btn" onClick={saveEmailNotif} disabled={savingEmail}>
                                    <FiCheck size={13} /> {savingEmail ? "Saving…" : "Save"}
                                </button>
                                <button className="gs-cancel-btn" onClick={() => { setEditEmail(false); setEmailNotif(group.allowEmailNotification ?? true); }}>
                                    <FiX size={13} /> Cancel
                                </button>
                            </div>
                        </div>
                    ) : (
                        <p className="gs-field-value">{emailNotif ? "On" : "Off"}</p>
                    )}
                </section>

                {/* Group image */}
                <section className="gs-section">
                    <div className="gs-section-header">
                        <span className="gs-section-label">Group image</span>
                    </div>
                    <div
                        className={`gs-image-row${imgDragOver ? " drag-over" : ""}`}
                        onDragOver={(e) => { e.preventDefault(); setImgDragOver(true); }}
                        onDragLeave={() => setImgDragOver(false)}
                        onDrop={(e) => {
                            e.preventDefault();
                            setImgDragOver(false);
                            const file = e.dataTransfer.files?.[0];
                            if (!file) return;
                            if (!file.type.startsWith("image/")) { showToast("Only image files are allowed"); return; }
                            if (isImageTooLarge(file)) { showToast(`Image must be under ${LIMITS.MAX_IMAGE_SIZE_MB} MB`); return; }
                            setCoverImage(file);
                        }}
                    >
                        {(group.imgUrl || group.defaultImgUrl) && (
                            <img
                                src={group.imgUrl ? blobBase + group.imgUrl : blobBase + group.defaultImgUrl}
                                alt="Group"
                                className="gs-group-thumb"
                            />
                        )}
                        <input
                            ref={fileRef}
                            type="file"
                            accept="image/*"
                            hidden
                            onChange={e => setCoverImage(e.target.files?.[0] || null)}
                        />
                        <button className="gs-upload-btn" onClick={() => fileRef.current?.click()}>
                            {coverImage ? coverImage.name : "Choose file"}
                        </button>
                        {coverImage && (
                            <button className="gs-save-btn" onClick={uploadImage} disabled={uploadingImg}>
                                <FiCheck size={13} /> {uploadingImg ? "Uploading…" : "Upload"}
                            </button>
                        )}
                    </div>
                </section>

                {/* Transfer leadership */}
                <section className="gs-section gs-section-warning">
                    <div className="gs-section-header">
                        <span className="gs-section-label">
                            <FiShield size={14} className="gs-icon-warning" /> Transfer leadership
                        </span>
                        {!showTransfer && (
                            <button className="gs-edit-btn gs-warning-btn" onClick={() => setShowTransfer(true)}>
                                Change
                            </button>
                        )}
                    </div>

                    {showTransfer && (
                        <div className="gs-transfer-area">
                            <div className="gs-transfer-search">
                                <FiSearch size={13} />
                                <input
                                    type="text"
                                    value={transferSearch}
                                    onChange={e => setTransferSearch(e.target.value)}
                                    placeholder="Search members…"
                                />
                            </div>
                            <div className="gs-transfer-list">
                                {filteredTransfer.length === 0 ? (
                                    <div className="muted" style={{ padding: "8px", fontSize: "13px" }}>No members found</div>
                                ) : filteredTransfer.map(m => (
                                    <div
                                        key={m.id}
                                        className={`gs-transfer-item${transferTarget?.id === m.id ? " selected" : ""}`}
                                        onClick={() => { setTransferTarget(m); setConfirmTransfer(false); }}
                                    >
                                        <img
                                            src={m.user?.imgUrl ? blobBase + m.user.imgUrl : (m.user?.defaultImgUrl ? blobBase + m.user.defaultImgUrl : "")}
                                            alt=""
                                            className="topbar-member-img"
                                        />
                                        <span>{m.user?.name || m.user?.email}</span>
                                        <span className="topbar-member-role">{m.role}</span>
                                    </div>
                                ))}
                            </div>

                            {transferTarget && !confirmTransfer && (
                                <div className="gs-field-actions">
                                    <button className="gs-danger-btn" onClick={() => setConfirmTransfer(true)}>
                                        Transfer to {transferTarget.user?.name || transferTarget.user?.email}
                                    </button>
                                    <button className="gs-cancel-btn" onClick={() => { setShowTransfer(false); setTransferTarget(null); }}>
                                        Cancel
                                    </button>
                                </div>
                            )}

                            {confirmTransfer && (
                                <div className="gs-confirm-box">
                                    <FiAlertTriangle size={16} className="gs-icon-danger" />
                                    <span>
                                        Are you sure? You will lose your leader role.
                                    </span>
                                    <div className="gs-field-actions">
                                        <button className="gs-danger-btn" onClick={handleTransfer} disabled={transferring}>
                                            {transferring ? "Transferring…" : "Yes, transfer"}
                                        </button>
                                        <button className="gs-cancel-btn" onClick={() => setConfirmTransfer(false)}>
                                            No
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </section>

                {/* Danger zone */}
                <section className="gs-section gs-section-danger">
                    <div className="gs-section-header">
                        <span className="gs-section-label">
                            <FiTrash2 size={14} className="gs-icon-danger" /> Delete group
                        </span>
                        {!confirmDelete && (
                            <button className="gs-danger-btn" onClick={() => setConfirmDelete(true)}>
                                Delete
                            </button>
                        )}
                    </div>
                    {confirmDelete && (
                        <div className="gs-confirm-box">
                            <FiAlertTriangle size={16} className="gs-icon-danger" />
                            <span>This action is permanent. All tasks and data will be lost.</span>
                            <div className="gs-field-actions">
                                <button className="gs-danger-btn" onClick={handleDelete} disabled={deleting}>
                                    {deleting ? "Deleting…" : "Confirm delete"}
                                </button>
                                <button className="gs-cancel-btn" onClick={() => setConfirmDelete(false)}>
                                    Cancel
                                </button>
                            </div>
                        </div>
                    )}
                </section>

                <div className="popup-actions" style={{ marginTop: 16 }}>
                    <button className="btn-secondary" onClick={onClose}>Close</button>
                </div>
            </div>
        </div>
    );
}
