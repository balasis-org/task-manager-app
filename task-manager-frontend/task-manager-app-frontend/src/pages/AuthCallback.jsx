import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import "@styles/pages/Login.css";

// handles the Azure AD redirect — exchanges the ?code for a session cookie
// then redirects to /dashboard
export default function AuthCallback() {
    const [searchParams] = useSearchParams();
    const [error, setError] = useState("");

    useEffect(() => {
        const code = searchParams.get("code");

        if (!code) {
            setError("No authorization code found in the URL.");
            return;
        }

        let cancelled = false;

        (async () => {
            try {
                const state = searchParams.get("state");
                const res = await fetch("/api/auth/exchange", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ code, state }),
                });

                if (!res.ok) {
                    if (res.status === 503) {
                        throw new Error(
                            "We are sorry but currently we are under heavy traffic. " +
                            "We cannot accept your request at this time, please try again later."
                        );
                    }
                    const text = await res.text().catch(() => "");
                    throw new Error(text || `HTTP ${res.status}`);
                }

                if (!cancelled) {
                    // Full reload so AuthProvider picks up the new cookies
                    window.location.href = "/dashboard";
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.message || "Authentication failed.");
                }
            }
        })();

        return () => { cancelled = true; };
    }, [searchParams]);

    if (error) {
        return (
            <div className="login-page">
                <div className="login-card">
                    <h1>Authentication Error</h1>
                    <div className="login-error">{error}</div>
                    <a href="/login" className="login-btn" style={{ textAlign: "center", textDecoration: "none", display: "block", marginTop: 16 }}>
                        Back to login
                    </a>
                </div>
            </div>
        );
    }

    return (
        <div className="login-page">
            <div className="login-card">
                <h1>Signing in…</h1>
                <p style={{ textAlign: "center", color: "#888" }}>Completing Azure AD authentication</p>
            </div>
        </div>
    );
}
