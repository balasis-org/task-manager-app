import { useContext, useEffect, useRef, useState } from "react";
import { NavLink, useNavigate, useLocation } from "react-router-dom";
import { FiGrid, FiMail, FiSliders, FiInfo, FiLogOut, FiShield } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import Footer from "@components/footer/Footer";
import "@styles/Layout.css";
import blobBase from "@blobBase";

export default function Layout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    const location = useLocation();
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [hasNewInvites, setHasNewInvites] = useState(false);
    const pollTimer = useRef(null);

    const INVITE_POLL_MS = 30_000; // 30 seconds

    // lightweight invitation check — polls every 20s
    // the backend returns 204 (no new) or 409 (has new invitations)
    useEffect(() => {
        if (!user) return;

        // if user is on the invitations page, clear the badge and skip polling
        const onInvitationsPage = location.pathname === "/invitations";
        if (onInvitationsPage) {
            setHasNewInvites(false);
            return;
        }

        let cancelled = false;

        async function check() {
            if (cancelled) return;
            try {
                await apiGet("/api/group-invitations/check-new");
                // 204 — no new invites
                if (!cancelled) setHasNewInvites(false);
            } catch (err) {
                // 409 means new invitations exist
                if (!cancelled && err?.status === 409) {
                    setHasNewInvites(true);
                }
                // other errors silently ignored
            }
        }

        check(); // immediate first check
        pollTimer.current = setInterval(check, INVITE_POLL_MS);

        return () => {
            cancelled = true;
            if (pollTimer.current) clearInterval(pollTimer.current);
        };
    }, [user?.id, location.pathname]);

    const handleLogout = async () => {
        await logout();
        navigate("/login");
    };

    const profileImgNoPrefix = user?.imgUrl || user?.defaultImgUrl  || null;
    const profileImg = blobBase + profileImgNoPrefix;

    return (
        <div className="layout">

            <aside className={`sidebar${sidebarOpen ? "" : " collapsed"}`}>
                <button
                    className="sidebar-toggle"
                    onClick={() => setSidebarOpen((v) => !v)}
                    title={sidebarOpen ? "Hide sidebar" : "Show sidebar"}
                >
                    {sidebarOpen ? "◀" : "▶"}
                </button>

                <div className="sidebar-clip">
                <div className="sidebar-inner">
                    {/* profile pic + name */}
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
                        {user?.name && (
                            <span className="sidebar-profile-name">{user.name}</span>
                        )}
                        {user?.email && (
                            <span className="sidebar-profile-email">{user.email}</span>
                        )}
                    </div>

                    {/* nav links */}
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
                        <button onClick={handleLogout}>
                            <span className="nav-icon"><FiLogOut size={16} /></span>
                            <span className="nav-label">Logout</span>
                        </button>
                    </nav>


                    <div className="sidebar-date">
                        {new Date().toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}
                    </div>


                    <div className="sidebar-footer">
                        <Footer />
                    </div>
                </div>
                </div>
            </aside>


            <div className="layout-body">
                <main className="layout-main">{children}</main>
            </div>
        </div>
    );
}
