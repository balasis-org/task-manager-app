import { useContext, useEffect, useState } from "react";
import { FiUser, FiMail, FiCheck, FiX } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext.jsx";
import { useToast } from "@context/ToastContext";
import { apiPatch } from "@assets/js/apiClient.js";
import { LIMITS } from "@assets/js/inputValidation";
import SettingsAvatar from "@components/settings/SettingsAvatar";
import SettingsInviteCode from "@components/settings/SettingsInviteCode";
import SettingsPreferences from "@components/settings/SettingsPreferences";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Settings.css";

export default function Settings() {
    const { user, setUser } = useContext(AuthContext);
    const showToast = useToast();

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
                        <span className="settings-value-ro">{user?.email || "—"}</span>
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
