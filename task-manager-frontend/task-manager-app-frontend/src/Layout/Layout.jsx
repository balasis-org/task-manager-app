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
    const lastActivityRef = useRef(Date.now());

    // Tiered polling for invitations — tripled cadence (there's a manual refresh)
    const TIER1_MS    = 60_000;          // 1 min active
    const TIER1_UNTIL = 10 * 60_000;     // first 10 min
    const TIER2_MS    = 3 * 60_000;      // 3 min mildly idle
    const TIER2_UNTIL = 15 * 60_000;     // 10–15 min
    const TIER3_MS    = 45 * 60_000;     // 45 min deep idle

    function getInvitePollInterval() {
        const idle = Date.now() - lastActivityRef.current;
        if (idle < TIER1_UNTIL) return TIER1_MS;
        if (idle < TIER2_UNTIL) return TIER2_MS;
        return TIER3_MS;
    }

    // track user activity to avoid polling when tab is backgrounded / idle
    useEffect(() => {
        const touch = () => { lastActivityRef.current = Date.now(); };
        window.addEventListener("mousemove", touch, { passive: true });
        window.addEventListener("keydown", touch, { passive: true });
        return () => {
            window.removeEventListener("mousemove", touch);
            window.removeEventListener("keydown", touch);
        };
    }, []);

    // Lightweight invitation polling — runs everywhere including the invitations page.
    // When NOT on the invitations page and new invites exist → show badge.
    // When ON the invitations page and new invites exist → dispatch custom event so the page re-fetches.
    useEffect(() => {
        if (!user) return;

        let cancelled = false;
        const onInvitationsPage = () => location.pathname === "/invitations";

        async function check() {
            if (cancelled) return;

            try {
                await apiGet("/api/group-invitations/check-new");
                // 204 — no new invites
                if (!cancelled) setHasNewInvites(false);
            } catch (err) {
                if (cancelled) return;
                if (err?.status === 409) {
                    if (onInvitationsPage()) {
                        // don't show badge; tell the Invitations page to re-fetch
                        setHasNewInvites(false);
                        window.dispatchEvent(new CustomEvent("invites-changed"));
                    } else {
                        setHasNewInvites(true);
                    }
                }
                // other errors silently ignored
            }

            if (!cancelled) schedulePoll();   // chain the next tick
        }

        function schedulePoll() {
            if (pollTimer.current) clearTimeout(pollTimer.current);
            pollTimer.current = setTimeout(check, getInvitePollInterval());
        }

        check(); // immediate first check

        return () => {
            cancelled = true;
            if (pollTimer.current) clearTimeout(pollTimer.current);
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
                        {user?.inviteCode && (
                            <span
                                className="sidebar-profile-code"
                                title="Your invite code — share it so others can invite you"
                                onClick={() => {
                                    navigator.clipboard.writeText(user.inviteCode);
                                }}
                            >
                                {user.inviteCode}
                            </span>
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
