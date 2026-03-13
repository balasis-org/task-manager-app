import { useContext, useEffect, useState } from "react";
import { FiUser, FiMail, FiCheck, FiX, FiHardDrive, FiDownloadCloud, FiLayers, FiImage } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { useToast } from "@context/ToastContext";
import { useTierUpgrade } from "@context/TierUpgradeContext";
import { apiGet, apiPatch } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import { formatFileSize } from "@assets/js/fileUtils";
import SettingsAvatar from "@components/settings/SettingsAvatar";
import SettingsInviteCode from "@components/settings/SettingsInviteCode";
import SettingsPreferences from "@components/settings/SettingsPreferences";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Settings.css";

export default function Settings() {
    const { user, setUser } = useContext(AuthContext);
    const showToast = useToast();
    const openTierUpgrade = useTierUpgrade();

    usePageTitle("Settings");

    const [name, setName] = useState(user?.name || "");
    const [emailNotif, setEmailNotif] = useState(user?.allowEmailNotification ?? true);
    const [darkMode, setDarkMode] = useState(
        () => document.documentElement.getAttribute("data-theme") === "dark"
    );

    const [serverName, setServerName] = useState(user?.name || "");
    const [serverNotif, setServerNotif] = useState(user?.allowEmailNotification ?? true);

    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (!user) return;
        setName(user.name || "");
        setEmailNotif(user.allowEmailNotification ?? true);
        setServerName(user.name || "");
        setServerNotif(user.allowEmailNotification ?? true);
    }, [user]);

    useEffect(() => {
        document.documentElement.setAttribute("data-theme", darkMode ? "dark" : "light");
        localStorage.setItem("theme", darkMode ? "dark" : "light");
    }, [darkMode]);

    useEffect(() => {
        const saved = localStorage.getItem("theme");
        if (saved === "dark") {
            setDarkMode(true);
            document.documentElement.setAttribute("data-theme", "dark");
        }
    }, []);

    useEffect(() => {
        apiGet("/api/users/me").then(setUser).catch(() => {});
    }, []);

    const hasChanges = name.trim() !== serverName || emailNotif !== serverNotif;

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

    return (
        <div className="settings-page">
            <h1 className="settings-title">Settings</h1>

            <div className="settings-grid">
                <SettingsAvatar />

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
                        <span className="settings-value-ro">{user?.email || "-"}</span>
                        <span className="settings-hint">Managed by your identity provider</span>
                    </div>

                    <SettingsInviteCode />
                </section>

                <SettingsPreferences
                    emailNotif={emailNotif}
                    onEmailNotifChange={setEmailNotif}
                    darkMode={darkMode}
                    onDarkModeChange={setDarkMode}
                />

                <section className="settings-card">
                    <h2 className="settings-card-heading">
                        <FiLayers size={16} /> Subscription Plan
                    </h2>
                    <div className="settings-storage-info">
                        <span className="settings-plan-badge">{user?.subscriptionPlan || "FREE"}</span>
                        <button className="settings-btn settings-btn-secondary settings-change-plan-btn" onClick={openTierUpgrade}>
                            View Plans
                        </button>
                    </div>
                </section>

                {/* storage usage — only shown for paid plans (budget > 0) */}
                {user?.storageBudgetBytes > 0 && (
                    <section className="settings-card">
                        <h2 className="settings-card-heading">
                            <FiHardDrive size={16} /> Storage
                        </h2>
                        <div className="settings-storage-info">
                            <span>
                                {formatFileSize(user.usedStorageBytes ?? 0)} / {formatFileSize(user.storageBudgetBytes)} used
                            </span>
                            <span className="settings-plan-badge">{user.subscriptionPlan}</span>
                        </div>
                        <div className="settings-storage-bar-track">
                            <div
                                className={`settings-storage-bar-fill${
                                    (user.usedStorageBytes ?? 0) / user.storageBudgetBytes > 0.9 ? " danger" : ""
                                }`}
                                style={{
                                    width: `${Math.min(100, ((user.usedStorageBytes ?? 0) / user.storageBudgetBytes) * 100)}%`,
                                }}
                            />
                        </div>
                    </section>
                )}

                {/* monthly download budget — shown for all plans */}
                {user?.downloadBudgetBytes > 0 && (
                    <section className="settings-card">
                        <h2 className="settings-card-heading">
                            <FiDownloadCloud size={16} /> Download Budget{" "}
                            <span className="settings-heading-hint">(for groups you own)</span>
                        </h2>
                        <div className="settings-storage-info">
                            <span>
                                {formatFileSize(user.usedDownloadBytesMonth ?? 0)} / {formatFileSize(user.downloadBudgetBytes)} used this month
                            </span>
                            <span className="settings-plan-badge">{user.subscriptionPlan}</span>
                        </div>
                        <div className="settings-storage-bar-track">
                            <div
                                className={`settings-storage-bar-fill${
                                    (user.usedDownloadBytesMonth ?? 0) / user.downloadBudgetBytes > 0.9 ? " danger" : ""
                                }`}
                                style={{
                                    width: `${Math.min(100, ((user.usedDownloadBytesMonth ?? 0) / user.downloadBudgetBytes) * 100)}%`,
                                }}
                            />
                        </div>
                    </section>
                )}

                {/* image uploads this month — paid plans only, discreet count */}
                {user?.imageScansPerMonth > 0 && (
                    <section className="settings-card">
                        <h2 className="settings-card-heading">
                            <FiImage size={16} /> Images Uploaded
                        </h2>
                        <div className="settings-storage-info">
                            <span className={
                                (user.usedImageScansMonth ?? 0) >= user.imageScansPerMonth
                                    ? "settings-image-count-limit" : ""
                            }>
                                {user.usedImageScansMonth ?? 0} this month
                            </span>
                        </div>
                        <span className="settings-image-hint">
                            Resets monthly. Choosing a default or your Microsoft image doesn&rsquo;t count.
                        </span>
                    </section>
                )}

                {/* monthly email quota — shown for plans with email budget */}
                {user?.emailsPerMonth > 0 && (
                    <section className="settings-card">
                        <h2 className="settings-card-heading">
                            <FiMail size={16} /> Email Quota{" "}
                            <span className="settings-heading-hint">(for groups you own)</span>
                        </h2>
                        <div className="settings-storage-info">
                            <span>
                                {user.usedEmailsMonth ?? 0} / {user.emailsPerMonth} sent this month
                            </span>
                            <span className="settings-plan-badge">{user.subscriptionPlan}</span>
                        </div>
                        <div className="settings-storage-bar-track">
                            <div
                                className={`settings-storage-bar-fill${
                                    (user.usedEmailsMonth ?? 0) / user.emailsPerMonth > 0.9 ? " danger" : ""
                                }`}
                                style={{
                                    width: `${Math.min(100, ((user.usedEmailsMonth ?? 0) / user.emailsPerMonth) * 100)}%`,
                                }}
                            />
                        </div>
                    </section>
                )}
            </div>

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
