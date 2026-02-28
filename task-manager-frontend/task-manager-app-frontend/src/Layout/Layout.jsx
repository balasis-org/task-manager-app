import { useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import { apiGet } from "@assets/js/apiClient.js";
import Footer from "@components/footer/Footer";
import SidebarProfile from "@components/layout/SidebarProfile";
import SidebarNav from "@components/layout/SidebarNav";
import "@styles/Layout.css";

export default function Layout({ children }) {
    const { user, logout } = useContext(AuthContext);
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
                    <SidebarProfile user={user} />

                    <SidebarNav
                        user={user}
                        hasNewInvites={hasNewInvites}
                        onLogout={handleLogout}
                    />

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
