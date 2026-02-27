import { useContext, useEffect, useRef, useState } from "react";
import { NavLink, useNavigate, useLocation } from "react-router-dom";
import { FiGrid, FiMail, FiSliders, FiInfo, FiLogOut, FiShield } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import Footer from "@components/footer/Footer";
import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/Layout.css";

export default function Layout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const blobUrl = useBlobUrl();
    const navigate = useNavigate();
    const location = useLocation();
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [hasNewInvites, setHasNewInvites] = useState(false);
    const pollTimer = useRef(null);
    const lastActivityRef = useRef(Date.now());

    const TIER1_MS    = 60_000;
    const TIER1_UNTIL = 10 * 60_000;
    const TIER2_MS    = 3 * 60_000;
    const TIER2_UNTIL = 15 * 60_000;
    const TIER3_MS    = 45 * 60_000;

    function getInvitePollInterval() {
        const idle = Date.now() - lastActivityRef.current;
        if (idle < TIER1_UNTIL) return TIER1_MS;
        if (idle < TIER2_UNTIL) return TIER2_MS;
        return TIER3_MS;
    }

    useEffect(() => {
        const touch = () => { lastActivityRef.current = Date.now(); };
        window.addEventListener("mousemove", touch, { passive: true });
        window.addEventListener("keydown", touch, { passive: true });
        return () => {
            window.removeEventListener("mousemove", touch);
            window.removeEventListener("keydown", touch);
        };
    }, []);

    useEffect(() => {
        if (!user) return;

        let cancelled = false;
        const onInvitationsPage = () => location.pathname === "/invitations";

        async function check() {
            if (cancelled) return;

            try {
                await apiGet("/api/group-invitations/check-new");

                if (!cancelled) setHasNewInvites(false);
            } catch (err) {
                if (cancelled) return;
                if (err?.status === 409) {
                    if (onInvitationsPage()) {

                        setHasNewInvites(false);
                        window.dispatchEvent(new CustomEvent("invites-changed"));
                    } else {
                        setHasNewInvites(true);
                    }
                }

            }

            if (!cancelled) schedulePoll();
        }

        function schedulePoll() {
            if (pollTimer.current) clearTimeout(pollTimer.current);
            pollTimer.current = setTimeout(check, getInvitePollInterval());
        }

        check();

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
    const profileImg = profileImgNoPrefix ? blobUrl(profileImgNoPrefix) : null;

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
                    { }
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

                    { }
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
