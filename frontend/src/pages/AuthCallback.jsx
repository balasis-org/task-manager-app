import { useEffect, useState } from "react";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/AuthCallback.css";

// OAuth2 hybrid flow callback: reads the authorization code, state, and
// front-channel id_token from the URL fragment (#), exchanges them via
// /api/auth/exchange (which sets httpOnly JWT + refresh cookies),
// then navigates to the saved returnUrl or /dashboard.
export default function AuthCallback() {
    const [error, setError] = useState("");

    usePageTitle("Signing in…");

    useEffect(() => {
        const hash = window.location.hash.substring(1);
        const params = new URLSearchParams(hash);

        const azureError = params.get("error");
        const errorDesc = params.get("error_description");
        if (azureError) {
            setError(`Authentication failed: ${azureError}${errorDesc ? " — " + errorDesc : ""}`);
            return;
        }

        const code = params.get("code");
        const state = params.get("state");
        const id_token = params.get("id_token");

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
                    body: JSON.stringify({ code, state, id_token }),
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
                    const returnUrl = sessionStorage.getItem("returnUrl") || "/dashboard";
                    sessionStorage.removeItem("returnUrl");
                    window.location.href = returnUrl;
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.message || "Authentication failed.");
                }
            }
        })();

        return () => { cancelled = true; };
    }, []);

    if (error) {
        return (
            <div className="callback-page">
                <div className="callback-error-card">
                    <h1>Authentication Error</h1>
                    <div className="callback-error-msg">{error}</div>
                    <a href="/login" className="callback-error-back">
                        Back to login
                    </a>
                </div>
            </div>
        );
    }

    return (
        <div className="callback-page">
            <div className="callback-logo-wrapper">
                <div className="callback-spinner" />
                <div className="callback-logo">
                    <img
                        src="/static_frontend/ico/favicon.ico"
                        alt="myteamtasks"
                    />
                </div>
            </div>

            <div className="callback-text">
                <p className="callback-title">Signing in…</p>
                <p className="callback-subtitle">
                    Completing authentication, please wait
                </p>
            </div>
        </div>
    );
}
