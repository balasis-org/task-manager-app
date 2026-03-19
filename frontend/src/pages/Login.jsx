import { useContext, useState } from "react";
import { Navigate, Link, useLocation } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import { apiPost } from "@assets/js/apiClient";
import { FiUsers, FiLogIn, FiTool } from "react-icons/fi";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/Login.css";

// login page: production uses Azure AD redirect (getLoginUrl), dev profiles
// show a fake-login panel with preset users across all subscription tiers.
// redirects to returnUrl after successful auth (saved in sessionStorage).
const IS_DEV = import.meta.env.DEV;

// GF=Free  GS=Student  GO=Organizer  GT=Team(stress)  GP=TeamsPro
const DEV_USERS = [
    { label: "Lena Dev",   email: "lena.dev@example.com",  role: "FREE · GF:leader · GT:member" },
    { label: "Marco Dev",  email: "marco.dev@example.com", role: "STUDENT · GS:leader · GT:tskMgr" },
    { label: "Nina Dev",   email: "nina.dev@example.com",  role: "ORGANIZER · GO:leader · GT:reviewer" },
    { label: "Tomas Dev",  email: "tomas.dev@example.com", role: "TEAM · GT:leader" },
    { label: "Alina Dev",  email: "alina.dev@example.com", role: "TEAMS_PRO · GP:leader" },
    { label: "Sofia Dev",  email: "sofia.dev@example.com", role: "STUDENT · GO:reviewer · GP:tskMgr · GT:member" },
    { label: "Peter Dev",  email: "peter.dev@example.com", role: "STUDENT · GO:member · GP:reviewer · GT:member" },
    { label: "Hanna Dev",  email: "hanna.dev@example.com", role: "STUDENT · GO:member · GP:member · GT:member" },
    { label: "Leon Dev",   email: "leon.dev@example.com",  role: "STUDENT · GO:tskMgr · GP:member · GT:member" },
    { label: "Erik Dev",   email: "erik.dev@example.com",  role: "FREE · GS:tskMgr · GT:member" },
    { label: "Julia Dev",  email: "julia.dev@example.com", role: "FREE · GS:reviewer · GT:member" },
    { label: "Ravi Dev",   email: "ravi.dev@example.com",  role: "FREE · GS:member · GT:member" },
    { label: "Katya Dev",  email: "katya.dev@example.com", role: "FREE · GS:member · GT:member" },
];

// stress-test users (stress01-stress02 shown in dropdown, rest exist in DB for k6)
const STRESS_USERS = Array.from({ length: 2 }, (_, i) => {
    const num = String(i + 1).padStart(2, "0");
    return {
        label: `Stress${num} Dev`,
        email: `stress${num}.dev@example.com`,
        role: "FREE · GT:member",
    };
});

export default function Login() {
    const { user, loading, authError } = useContext(AuthContext);
    const location = useLocation();
    const storedReturn = sessionStorage.getItem("returnUrl");
    const returnUrl = storedReturn || location.state?.returnUrl || "/dashboard";

    const [devOpen, setDevOpen] = useState(false);
    const [fakeEmail, setFakeEmail] = useState(DEV_USERS[0].email);
    const [fakePlan, setFakePlan] = useState("");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    usePageTitle("Sign in");

    if (!loading && user) {
        sessionStorage.removeItem("returnUrl");
        return <Navigate to={returnUrl} replace />;
    }
    if (loading) return null;

    const handleFakeLogin = async (e) => {
        e.preventDefault();
        setError("");
        setBusy(true);
        try {
            const name = fakeEmail.split("@")[0].replace(".dev", "");
            const body = { email: fakeEmail, name };
            if (fakePlan) body.subscriptionPlan = fakePlan;
            await apiPost("/api/auth/fake-login", body);
            if (returnUrl && returnUrl !== "/dashboard") {
                sessionStorage.setItem("returnUrl", returnUrl);
            }
            window.location.reload();
        } catch (err) {
            setError(
                err?.status === 503
                    ? err.message
                    : "Login failed. Please check your credentials."
            );
        } finally {
            setBusy(false);
        }
    };

    const handleAzureLogin = async () => {
        try {
            const res = await fetch("/api/auth/login-url");
            const url = await res.text();
            if (url) {
                if (returnUrl && returnUrl !== "/dashboard") {
                    sessionStorage.setItem("returnUrl", returnUrl);
                }
                window.location.href = url;
            }
        } catch {
            setError("Could not get login URL.");
        }
    };

    return (
        <div className="login-page">
            {IS_DEV && (
                <button
                    type="button"
                    className="login-dev-toggle"
                    onClick={() => setDevOpen((v) => !v)}
                    aria-label="Toggle dev panel"
                    title="Dev tools"
                >
                    <FiTool size={14} />
                </button>
            )}

            <div className="login-hero" aria-hidden="true">
                <div className="login-hero-content">
                    <p className="login-hero-title">myteamtasks</p>
                    <p className="login-hero-tagline">
                        Where teams turn plans into progress
                    </p>
                </div>
            </div>

            <div className="login-card-panel">
            <div className="login-card">
                <div className="login-brand">
                    <div className="login-brand-icon">
                        <img src="/static_frontend/ico/favicon.ico" alt="" />
                    </div>
                    <h1>Sign in</h1>
                    <p className="login-subtitle">Access your team workspace</p>
                </div>

                {(error || authError) && <div className="login-error">{error || authError}</div>}

                <button
                    type="button"
                    className="login-btn login-btn-ms"
                    onClick={handleAzureLogin}
                    disabled={busy}
                >
                    <FiLogIn className="login-btn-icon" />
                    Sign in with Microsoft
                </button>

                {IS_DEV && devOpen && (
                    <>
                        <div className="login-divider"><span>dev only</span></div>

                        <form onSubmit={handleFakeLogin} className="login-dev-form">
                            <label className="login-field">
                                <span className="login-field-label">
                                    <FiUsers size={13} /> Test user
                                </span>
                                <select
                                    value={fakeEmail}
                                    onChange={(e) => setFakeEmail(e.target.value)}
                                >
                                    <optgroup label="Core users">
                                        {DEV_USERS.map((u) => (
                                            <option key={u.email} value={u.email}>
                                                {u.label} {u.role}
                                            </option>
                                        ))}
                                    </optgroup>
                                    <optgroup label="Stress-test users (GT)">
                                        {STRESS_USERS.map((u) => (
                                            <option key={u.email} value={u.email}>
                                                {u.label} {u.role}
                                            </option>
                                        ))}
                                    </optgroup>
                                </select>
                            </label>

                            <label className="login-field">
                                <span className="login-field-label">
                                    Subscription plan override
                                </span>
                                <select
                                    value={fakePlan}
                                    onChange={(e) => setFakePlan(e.target.value)}
                                >
                                    <option value="">Default (keep existing)</option>
                                    <option value="FREE">FREE</option>
                                    <option value="STUDENT">STUDENT</option>
                                    <option value="ORGANIZER">ORGANIZER</option>
                                    <option value="TEAM">TEAM</option>
                                    <option value="TEAMS_PRO">TEAMS_PRO</option>
                                </select>
                            </label>

                            <button
                                type="submit"
                                className="login-btn login-btn-dev"
                                disabled={busy}
                            >
                                {busy ? "Signing in…" : "Quick sign in"}
                            </button>
                        </form>
                    </>
                )}

                <div className="login-legal">
                    <Link to="/terms-of-service">Terms of Service</Link>
                    <span className="login-legal-dot">·</span>
                    <Link to="/cookie-policy">Cookie Policy</Link>
                </div>
            </div>
            </div>
        </div>
    );
}
