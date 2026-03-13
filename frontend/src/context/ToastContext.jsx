import { createContext, useContext, useState, useCallback, useRef } from "react";
import "@styles/Toast.css";

const ToastContext = createContext(null);

let nextId = 1;

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);
    const timers = useRef({});

    const removeToast = useCallback((id) => {
        clearTimeout(timers.current[id]);
        delete timers.current[id];
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const showToast = useCallback((message, type = "error", duration = 4000) => {
        const id = nextId++;
        setToasts((prev) => [...prev, { id, message, type }]);
        timers.current[id] = setTimeout(() => removeToast(id), duration);
    }, [removeToast]);

    return (
        <ToastContext.Provider value={showToast}>
            {children}
            <div className="toast-container">
                {toasts.map((t) => (
                    <div key={t.id} className={`toast toast-${t.type}`} onClick={() => removeToast(t.id)}>
                        <span className="toast-icon">
                            {t.type === "success" ? "✓" : t.type === "error" ? "✕" : "ℹ"}
                        </span>
                        <span className="toast-msg">{t.message}</span>
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    );
}

export function useToast() {
    const ctx = useContext(ToastContext);
    if (!ctx) throw new Error("useToast must be used inside ToastProvider");
    return ctx;
}
