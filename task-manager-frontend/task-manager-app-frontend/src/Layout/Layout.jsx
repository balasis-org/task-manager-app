import { useContext, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { FiGrid, FiMail, FiSliders, FiInfo, FiLogOut } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import Footer from "@components/footer/Footer";
import "@styles/Layout.css";
import blobBase from "@blobBase";

export default function Layout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(true);

    const handleLogout = async () => {
        await logout();
        navigate("/login");
    };

    const profileImgNoPrefix = user?.imgUrl || user?.defaultImgUrl  || null;
    const profileImg = blobBase + profileImgNoPrefix;

    return (
        <div className="layout">
            {/* ── Sidebar ── */}
            <aside className={`sidebar${sidebarOpen ? "" : " collapsed"}`}>
                <button
                    className="sidebar-toggle"
                    onClick={() => setSidebarOpen((v) => !v)}
                    title={sidebarOpen ? "Hide sidebar" : "Show sidebar"}
                >
                    {sidebarOpen ? "◀" : "▶"}
                </button>

                <div className="sidebar-inner">
                    {sidebarOpen && (
                        <>
                            {/* Profile area */}
                            <div className="sidebar-profile">
                                {profileImg ? (
                                    <img
                                        src={profileImg}
                                        alt="profile"
                                        className="sidebar-profile-img"
                                    />
                                ) : (
                                    <div className="sidebar-profile-img" />
                                )}
                            </div>

                            {/* Navigation */}
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
                                <button onClick={handleLogout}>
                                    <span className="nav-icon"><FiLogOut size={16} /></span>
                                    <span className="nav-label">Logout</span>
                                </button>
                            </nav>

                            {/* Footer in sidebar bottom */}
                            <div className="sidebar-footer">
                                <Footer />
                            </div>
                        </>
                    )}
                </div>
            </aside>

            {/* ── Main content ── */}
            <div className="layout-body">
                <main className="layout-main">{children}</main>
            </div>
        </div>
    );
}
