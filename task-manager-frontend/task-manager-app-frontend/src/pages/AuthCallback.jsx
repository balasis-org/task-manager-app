import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import usePageTitle from "@hooks/usePageTitle";
import "@styles/pages/AuthCallback.css";

export default function AuthCallback() {
    const [searchParams] = useSearchParams();
    const [error, setError] = useState("");

    usePageTitle("Signing in…");

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
