import { useContext, useState } from "react";
import { Navigate } from "react-router-dom";
import { AuthContext } from "@context/AuthContext";
import { apiPost } from "@assets/js/apiClient";
import "@styles/pages/Login.css";

export default function Login() {
    const { user, loading } = useContext(AuthContext);
    const [fakeEmail, setFakeEmail] = useState("alice.dev@example.com");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    // Already authenticated → go to dashboard
    if (!loading && user) {
        return <Navigate to="/dashboard" replace />;
    }

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
            <div className="login-card">
                <h1>Task Manager</h1>
                <p className="login-subtitle">Sign in to continue</p>

                {error && <div className="login-error">{error}</div>}

                <form onSubmit={handleFakeLogin} className="login-form">
                    <label>
                        Fake user (dev only)
                        <select
                            value={fakeEmail}
                            onChange={(e) => setFakeEmail(e.target.value)}
                        >
                            <option value="alice.dev@example.com">Alice Dev</option>
                            <option value="bob.dev@example.com">Bob Dev</option>
                            <option value="carol.dev@example.com">Carol Dev</option>
                            <option value="dave.dev@example.com">Dave Dev</option>
                            <option value="erin.dev@example.com">Erin Dev</option>
                            <option value="frank.dev@example.com">Frank Dev</option>
                            <option value="grace.dev@example.com">Grace Dev</option>
                            <option value="heidi.dev@example.com">Heidi Dev</option>
                            <option value="ivan.dev@example.com">Ivan Dev</option>
                            <option value="judy.dev@example.com">Judy Dev</option>
                        </select>
                    </label>

                    <button type="submit" className="login-btn" disabled={busy}>
                        {busy ? "Signing in…" : "Sign in as dev"}
                    </button>
                </form>

                <div className="login-divider">
                    <span>or</span>
                </div>

                <button
                    type="button"
                    className="login-btn login-btn-azure"
                    onClick={handleAzureLogin}
                >
                    Sign in with Azure AD
                </button>
            </div>
        </div>
    );
}