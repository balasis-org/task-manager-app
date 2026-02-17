import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import "@styles/pages/Login.css";

/**
 * Azure AD redirect handler.
 *
 * Flow:
 *   1. User clicks "Sign in with Azure AD" → `GET /api/auth/login-url` → browser redirects to Azure.
 *   2. Azure authenticates → redirects back to `{redirect_uri}?code=xxx`.
 *   3. This component reads `code` from query-string, POSTs it to `/api/auth/exchange`.
 *   4. Backend sets HttpOnly JWT + RefreshKey cookies and returns success.
 *   5. Full-page redirect to `/dashboard` so AuthProvider re-bootstraps with the new cookies.
 *
 * The backend `redirect_uri` env-var (dev: `auth-redirectUri`) must point here,
 * e.g. `http://localhost:5173/auth/callback` (dev) or `https://frontend.example.com/auth/callback` (prod).
 */
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
                const res = await fetch("/api/auth/exchange", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ code }),
                });

                if (!res.ok) {
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
