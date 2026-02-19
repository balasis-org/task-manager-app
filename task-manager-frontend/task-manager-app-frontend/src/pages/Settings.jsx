import { useContext, useEffect, useRef, useState } from "react";
import { FiCamera, FiUser, FiMail, FiBell, FiMoon, FiCheck, FiX, FiCopy, FiRefreshCw } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { useToast } from "@context/ToastContext";
import { apiGet, apiPatch, apiMultipart, apiPost } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import { isImageTooLarge } from "@assets/js/fileUtils";
import blobBase from "@blobBase";
import "@styles/pages/Settings.css";

export default function Settings() {
    const { user, setUser } = useContext(AuthContext);
    const showToast = useToast();

    // local form state
    const [name, setName] = useState(user?.name || "");
    const [emailNotif, setEmailNotif] = useState(user?.allowEmailNotification ?? true);
    const [darkMode, setDarkMode] = useState(
        () => document.documentElement.getAttribute("data-theme") === "dark"
    );

    // track what the server says so we know if anything changed
    const [serverName, setServerName] = useState(user?.name || "");
    const [serverNotif, setServerNotif] = useState(user?.allowEmailNotification ?? true);

    const [saving, setSaving] = useState(false);
    const [uploadingImg, setUploadingImg] = useState(false);
    const [imgDragOver, setImgDragOver] = useState(false);
    const [refreshingCode, setRefreshingCode] = useState(false);
    const fileRef = useRef(null);

    // sync when user context changes (e.g. after login)
    useEffect(() => {
        if (!user) return;
        setName(user.name || "");
        setEmailNotif(user.allowEmailNotification ?? true);
        setServerName(user.name || "");
        setServerNotif(user.allowEmailNotification ?? true);
    }, [user]);

    // persist dark mode preference
    useEffect(() => {
        document.documentElement.setAttribute("data-theme", darkMode ? "dark" : "light");
        localStorage.setItem("theme", darkMode ? "dark" : "light");
    }, [darkMode]);

    // load theme on mount
    useEffect(() => {
        const saved = localStorage.getItem("theme");
        if (saved === "dark") {
            setDarkMode(true);
            document.documentElement.setAttribute("data-theme", "dark");
        }
    }, []);

    const hasChanges = name.trim() !== serverName || emailNotif !== serverNotif;

    // --- profile image ---
    function handleImagePick(file) {
        if (!file) return;
        if (!file.type.startsWith("image/")) {
            showToast("Only image files are allowed");
            return;
        }
        if (file.type === "image/gif") {
            showToast("GIF images are not supported. Please use PNG or JPG.");
            return;
        }
        if (isImageTooLarge(file)) {
            showToast(`Image must be under ${LIMITS.MAX_IMAGE_SIZE_MB} MB`);
            return;
        }
        uploadImage(file);
    }

    async function uploadImage(file) {
        setUploadingImg(true);
        try {
            const fd = new FormData();
            fd.append("file", file);
            const updated = await apiMultipart("/api/users/me/profile-image", fd);
            setUser((prev) => (prev ? { ...prev, imgUrl: updated.imgUrl } : prev));
            showToast("Profile image updated!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to upload image");
        } finally {
            setUploadingImg(false);
        }
    }

    // --- save name + email notif ---
    async function handleSave() {
        if (!name.trim()) {
            showToast("Name cannot be empty");
            return;
        }
        setSaving(true);
        try {
            const updated = await apiPatch("/api/users/me", {
                name: name.trim(),
                allowEmailNotification: emailNotif,
            });
            setUser((prev) => (prev ? { ...prev, ...updated } : prev));
            setServerName(updated.name);
            setServerNotif(updated.allowEmailNotification);
            showToast("Settings saved!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to save settings");
        } finally {
            setSaving(false);
        }
    }

    function handleRevert() {
        setName(serverName);
        setEmailNotif(serverNotif);
    }

    async function handleRefreshCode() {
        setRefreshingCode(true);
        try {
            const updated = await apiPost("/api/users/me/refresh-invite-code");
            setUser((prev) => (prev ? { ...prev, inviteCode: updated.inviteCode } : prev));
            showToast("Invite code refreshed!", "success");
        } catch (err) {
            showToast(err?.message || "Failed to refresh invite code");
        } finally {
            setRefreshingCode(false);
        }
    }

    function handleCopyCode() {
        if (user?.inviteCode) {
            navigator.clipboard.writeText(user.inviteCode);
            showToast("Invite code copied!", "success");
        }
    }

    const imgSrc = user?.imgUrl
        ? blobBase + user.imgUrl
        : user?.defaultImgUrl
            ? blobBase + user.defaultImgUrl
            : null;

    return (
        <div className="settings-page">
            <h1 className="settings-title">Settings</h1>

            <div className="settings-grid">
                {/* profile image card */}
                <section className="settings-card settings-card-avatar">
                    <div
                        className={`settings-avatar-zone${imgDragOver ? " drag-over" : ""}`}
                        onDragOver={(e) => { e.preventDefault(); setImgDragOver(true); }}
                        onDragLeave={() => setImgDragOver(false)}
                        onDrop={(e) => {
                            e.preventDefault();
                            setImgDragOver(false);
                            handleImagePick(e.dataTransfer.files?.[0]);
                        }}
                        onClick={() => fileRef.current?.click()}
                        title="Click or drop an image to change your photo"
                    >
                        {imgSrc ? (
                            <img src={imgSrc} alt="Profile" className="settings-avatar-img" />
                        ) : (
                            <div className="settings-avatar-placeholder">
                                <FiUser size={40} />
                            </div>
                        )}
                        <div className="settings-avatar-overlay">
                            {uploadingImg ? (
                                <span className="settings-avatar-spinner" />
                            ) : (
                                <FiCamera size={22} />
                            )}
                        </div>
                        <input
                            ref={fileRef}
                            type="file"
                            accept="image/png, image/jpeg, image/webp"
                            hidden
                            onChange={(e) => {
                                handleImagePick(e.target.files?.[0]);
                                e.target.value = "";
                            }}
                        />
                    </div>
                    <span className="settings-avatar-hint">Click or drag to change photo</span>
                </section>

                {/* account details card */}
                <section className="settings-card">
                    <h2 className="settings-card-heading">
                        <FiUser size={16} /> Account
                    </h2>

                    <label className="settings-field">
                        <span className="settings-label">Display name</span>
                        <input
                            type="text"
                            className="settings-input"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            maxLength={LIMITS.USER_NAME}
                            placeholder="Your name"
                        />
                        <span className="settings-char-count">
                            {name.length}/{LIMITS.USER_NAME}
                        </span>
                    </label>

                    <div className="settings-field">
                        <span className="settings-label">
                            <FiMail size={13} /> Email
                        </span>
                        <span className="settings-value-ro">{user?.email || "—"}</span>
                        <span className="settings-hint">Managed by your identity provider</span>
                    </div>

                    <div className="settings-field">
                        <span className="settings-label">Invite code</span>
                        <div className="settings-invite-code-row">
                            <span className="settings-invite-code">{user?.inviteCode || "—"}</span>
                            <button
                                type="button"
                                className="settings-code-btn"
                                onClick={handleCopyCode}
                                title="Copy code"
                                disabled={!user?.inviteCode}
                            >
                                <FiCopy size={14} />
                            </button>
                            <button
                                type="button"
                                className="settings-code-btn"
                                onClick={handleRefreshCode}
                                title="Generate a new code"
                                disabled={refreshingCode}
                            >
                                <FiRefreshCw size={14} className={refreshingCode ? "spin" : ""} />
                            </button>
                        </div>
                        <span className="settings-hint">
                            Share this code with others so they can invite you to a group.
                            Refresh it to invalidate the old one.
                        </span>
                    </div>
                </section>

                {/* preferences card */}
                <section className="settings-card">
                    <h2 className="settings-card-heading">
                        <FiMoon size={16} /> Preferences
                    </h2>

                    <div className="settings-toggle-row">
                        <div className="settings-toggle-info">
                            <FiBell size={15} />
                            <div>
                                <span className="settings-toggle-label">Email notifications</span>
                                <span className="settings-toggle-desc">
                                    Receive emails when you're assigned or mentioned
                                </span>
                            </div>
                        </div>
                        <button
                            type="button"
                            className={`settings-switch${emailNotif ? " on" : ""}`}
                            onClick={() => setEmailNotif((v) => !v)}
                            aria-label="Toggle email notifications"
                        >
                            <span className="settings-switch-thumb" />
                        </button>
                    </div>

                    <div className="settings-toggle-row">
                        <div className="settings-toggle-info">
                            <FiMoon size={15} />
                            <div>
                                <span className="settings-toggle-label">Dark mode</span>
                                <span className="settings-toggle-desc">
                                    Switch between light and dark appearance
                                </span>
                            </div>
                        </div>
                        <button
                            type="button"
                            className={`settings-switch${darkMode ? " on" : ""}`}
                            onClick={() => setDarkMode((v) => !v)}
                            aria-label="Toggle dark mode"
                        >
                            <span className="settings-switch-thumb" />
                        </button>
                    </div>
                </section>
            </div>

            {/* sticky save bar */}
            {hasChanges && (
                <div className="settings-save-bar">
                    <span className="settings-save-hint">You have unsaved changes</span>
                    <div className="settings-save-actions">
                        <button
                            className="settings-btn settings-btn-secondary"
                            onClick={handleRevert}
                            disabled={saving}
                        >
                            <FiX size={14} /> Revert
                        </button>
                        <button
                            className="settings-btn settings-btn-primary"
                            onClick={handleSave}
                            disabled={saving}
                        >
                            <FiCheck size={14} /> {saving ? "Saving…" : "Save changes"}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}