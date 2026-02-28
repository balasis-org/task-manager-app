// Tab strip — owns the TABS array so the parent doesn't have to
import { FiUsers, FiLayers, FiCheckSquare, FiMessageSquare } from "react-icons/fi";
import "@styles/admin/AdminTabBar.css";

const TABS = [
    { key: "users",    label: "Users",    icon: <FiUsers size={15} /> },
    { key: "groups",   label: "Groups",   icon: <FiLayers size={15} /> },
    { key: "tasks",    label: "Tasks",    icon: <FiCheckSquare size={15} /> },
    { key: "comments", label: "Comments", icon: <FiMessageSquare size={15} /> },
];

export default function AdminTabBar({ activeTab, onSwitch }) {
    return (
        <div className="admin-tabs">
            {TABS.map((t) => (
                <button
                    key={t.key}
                    className={`admin-tab${activeTab === t.key ? " active" : ""}`}
                    onClick={() => onSwitch(t.key)}
                >
                    {t.icon}
                    <span>{t.label}</span>
                </button>
            ))}
        </div>
    );
}
