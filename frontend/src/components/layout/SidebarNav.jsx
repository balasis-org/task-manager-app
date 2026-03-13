import { NavLink } from "react-router-dom";
import { FiGrid, FiMail, FiSliders, FiInfo, FiLogOut, FiShield } from "react-icons/fi";
import "@styles/layout/SidebarNav.css";

export default function SidebarNav({ user, hasNewInvites, onLogout }) {
    return (
        <nav className="sidebar-nav">
            <NavLink
                to="/dashboard"
                className={({ isActive }) => (isActive ? "active" : "")}
            >
                <span className="nav-icon"><FiGrid size={16} /></span>
                <span className="nav-label">Dashboard</span>
            </NavLink>
            <NavLink
                to="/invitations"
                className={({ isActive }) => (isActive ? "active" : "")}
            >
                <span className="nav-icon"><FiMail size={16} /></span>
                <span className="nav-label">Invitations</span>
                {hasNewInvites && (
                    <span className="nav-badge">!</span>
                )}
            </NavLink>
            <NavLink
                to="/settings"
                className={({ isActive }) => (isActive ? "active" : "")}
            >
                <span className="nav-icon"><FiSliders size={16} /></span>
                <span className="nav-label">Settings</span>
            </NavLink>
            <NavLink
                to="/about-us"
                className={({ isActive }) => (isActive ? "active" : "")}
            >
                <span className="nav-icon"><FiInfo size={16} /></span>
                <span className="nav-label">About us</span>
            </NavLink>
            {user?.systemRole === "ADMIN" && (
                <NavLink
                    to="/admin"
                    className={({ isActive }) => (isActive ? "active" : "")}
                >
                    <span className="nav-icon"><FiShield size={16} /></span>
                    <span className="nav-label">Admin Panel</span>
                </NavLink>
            )}
            <button onClick={onLogout}>
                <span className="nav-icon"><FiLogOut size={16} /></span>
                <span className="nav-label">Logout</span>
            </button>
        </nav>
    );
}
