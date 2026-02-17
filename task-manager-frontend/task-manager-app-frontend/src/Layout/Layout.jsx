import { useContext, useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { FiGrid, FiMail, FiSliders, FiInfo, FiLogOut } from "react-icons/fi";
import { AuthContext } from "@context/AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import Footer from "@components/footer/Footer";
import "@styles/Layout.css";
import blobBase from "@blobBase";

export default function Layout({ children }) {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [newInviteCount, setNewInviteCount] = useState(0);

    // check if there are new invitations we havent seen
    useEffect(() => {
        if (!user) return;
        let cancelled = false;

        (async () => {
            try {
                const lastSeen = user.lastSeenInvites
                    ? new Date(user.lastSeenInvites)
                    : null;

                const invites = await apiGet("/api/group-invitations/me");
                if (cancelled) return;

                const pending = (Array.isArray(invites) ? invites : []).filter(
                    (inv) => inv.invitationStatus === "PENDING"
                );

                if (!lastSeen) {
                    // Never visited invitations page, all pending count as new
                    setNewInviteCount(pending.length);
                } else {
                    const unseen = pending.filter(
                        (inv) => inv.createdAt && new Date(inv.createdAt) > lastSeen
                    );
                    setNewInviteCount(unseen.length);
                }
            } catch {
                // silently ignore
            }
        })();

        return () => { cancelled = true; };
    }, [user?.id]);

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
                            {newInviteCount > 0 && (
                                <span className="nav-badge">{newInviteCount}</span>
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
