import { Component } from "react";

class AppShield extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    componentDidCatch(error, info) {

        console.error("AppShield caught:", error, info);
    }

    handleReload = () => {
        this.setState({ hasError: false });
        window.location.href = "/";
    };

    render() {
        if (this.state.hasError) {
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
                    <h1 style={{ fontSize: "1.5rem", margin: 0 }}>Something went wrong</h1>
                    <p style={{ color: "var(--text-secondary, #666)", margin: 0 }}>
                        An unexpected error occurred. Please try again.
                    </p>
                    <button
                        onClick={this.handleReload}
                        style={{
                            padding: "0.5rem 1.5rem",
                            borderRadius: "6px",
                            border: "1px solid var(--border-color, #ccc)",
                            background: "var(--btn-primary-bg, #4f46e5)",
                            color: "var(--btn-primary-text, #fff)",
                            cursor: "pointer",
                            fontSize: "0.95rem",
                        }}
                    >
                        Back to Dashboard
                    </button>
                </div>
            );
        }
        return this.props.children;
    }
}

export default AppShield;
