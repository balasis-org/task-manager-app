import { useContext, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
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
                                Dashboard
                            </NavLink>
                            <NavLink
                                to="/invitations"
                                className={({ isActive }) => (isActive ? "active" : "")}
                            >
                                Invitations
                            </NavLink>
                            <NavLink
                                to="/settings"
                                className={({ isActive }) => (isActive ? "active" : "")}
                            >
                                Settings
                            </NavLink>
                            <NavLink
                                to="/about-us"
                                className={({ isActive }) => (isActive ? "active" : "")}
                            >
                                About us
                            </NavLink>
                            <button onClick={handleLogout}>Logout</button>
                        </nav>

                        {/* Footer in sidebar bottom */}
                        <div className="sidebar-footer">
                            <Footer />
                        </div>
                    </>
                )}
            </aside>

            {/* ── Main content ── */}
            <div className="layout-body">
                <main className="layout-main">{children}</main>
            </div>
        </div>
    );
}
