import { FiBell, FiMoon } from "react-icons/fi";
import "@styles/settings/SettingsPreferences.css";

export default function SettingsPreferences({
    emailNotif,
    onEmailNotifChange,
    darkMode,
    onDarkModeChange,
}) {
    return (
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
                    onClick={() => onEmailNotifChange(!emailNotif)}
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
                    onClick={() => onDarkModeChange(!darkMode)}
                    aria-label="Toggle dark mode"
                >
                    <span className="settings-switch-thumb" />
                </button>
            </div>
        </section>
    );
}
