import { Link } from "react-router-dom";
import usePageTitle from "@hooks/usePageTitle";

export default function NotFound() {
    usePageTitle("Page not found");

    return (
        <div style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            minHeight: "100vh",
            gap: "1rem",
            fontFamily: "inherit",
            padding: "2rem",
            textAlign: "center",
        }}>
            <h1 style={{ fontSize: "3rem", margin: 0 }}>404</h1>
            <p style={{ color: "var(--text-secondary, #666)", margin: 0 }}>
                Page not found. The link may be broken or the page may have been removed.
            </p>
            <Link
                to="/"
                style={{
                    padding: "0.5rem 1.5rem",
                    borderRadius: "6px",
                    border: "1px solid var(--border-color, #ccc)",
                    background: "var(--btn-primary-bg, #4f46e5)",
                    color: "var(--btn-primary-text, #fff)",
                    textDecoration: "none",
                    fontSize: "0.95rem",
                }}
            >
                Back to Dashboard
            </Link>
        </div>
    );
}
