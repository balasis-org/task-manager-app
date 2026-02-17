import { useContext, useState } from "react";
import { Navigate, Link } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import { apiPost } from "@assets/js/apiClient";
import { FiShield, FiUsers, FiLogIn, FiTool } from "react-icons/fi";
import "@styles/pages/Login.css";

const IS_DEV = import.meta.env.DEV;

const DEV_USERS = [
    { label: "Alice Dev",  email: "alice.dev@example.com" },
    { label: "Bob Dev",    email: "bob.dev@example.com" },
    { label: "Carol Dev",  email: "carol.dev@example.com" },
    { label: "Dave Dev",   email: "dave.dev@example.com" },
    { label: "Erin Dev",   email: "erin.dev@example.com" },
    { label: "Frank Dev",  email: "frank.dev@example.com" },
    { label: "Grace Dev",  email: "grace.dev@example.com" },
    { label: "Heidi Dev",  email: "heidi.dev@example.com" },
    { label: "Ivan Dev",   email: "ivan.dev@example.com" },
    { label: "Judy Dev",   email: "judy.dev@example.com" },
];

export default function Login() {
    const { user, loading } = useContext(AuthContext);

    const [devOpen, setDevOpen] = useState(false);
    const [fakeEmail, setFakeEmail] = useState(DEV_USERS[0].email);
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    if (!loading && user) return <Navigate to="/dashboard" replace />;
    if (loading) return null;

    const handleFakeLogin = async (e) => {
        e.preventDefault();
        setError("");
        setBusy(true);
        try {
            const name = fakeEmail.split("@")[0].replace(".dev", "");
            await apiPost("/api/auth/fake-login", { email: fakeEmail, name });
            window.location.reload();
        } catch {
            setError("Login failed. Please check your credentials.");
        } finally {
            setBusy(false);
        }
    };

    const handleAzureLogin = async () => {
        try {
            const res = await fetch("/api/auth/login-url");
            const url = await res.text();
            if (url) window.location.href = url;
        } catch {
            setError("Could not get login URL.");
        }
    };

    return (
        <div className="login-page">
            {/* Dev-only toggle in the corner */}
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

            <div className="login-card">
                <div className="login-brand">
                    <div className="login-brand-icon">
                        <FiShield size={28} />
                    </div>
                    <h1>Task Manager</h1>
                    <p className="login-subtitle">Sign in to your account</p>
                </div>

                {error && <div className="login-error">{error}</div>}

                <button
                    type="button"
                    className="login-btn login-btn-ms"
                    onClick={handleAzureLogin}
                    disabled={busy}
                >
                    <FiLogIn className="login-btn-icon" />
                    Sign in with Microsoft
                </button>

                {/* Dev-only quick login panel */}
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
                                    {DEV_USERS.map((u) => (
                                        <option key={u.email} value={u.email}>
                                            {u.label}
                                        </option>
                                    ))}
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
    );
}