import { useEffect, useState } from "react";
import { AuthContext } from "./AuthContext";
import { apiGet, apiPost, registerAuthHandlers } from "@assets/js/apiClient.js";
import { useToast } from "@context/ToastContext";

let isRefreshing = false;

export default function AuthProvider({ children }) {
    const showToast = useToast();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [bootstrapped, setBootstrapped] = useState(false);
    const [authError, setAuthError] = useState(null);

    useEffect(() => {
        const loadUser = async () => {
            try {
                const me = await apiGet("/api/users/me");
                setUser(me);
                setAuthError(null);
            } catch (err) {
                setUser(null);
                if (err?.status === 503) {
                    const msg = err.message || "Service temporarily unavailable. Please try again later.";
                    setAuthError(msg);
                    showToast(msg, "error", 6000);
                }
            } finally {
                setLoading(false);
                setBootstrapped(true);
            }
        };

        loadUser();

        registerAuthHandlers({
            onUnauthorized: async (config) => {
                if (
                    config?.url?.startsWith("/api/auth") ||
                    config?.url === "/api/users/me"
                ) {
                    return false;
                }

                // The JwtInterceptor on the backend handles refresh transparently:
                // when the JWT cookie expires but the RefreshKey cookie is still valid,
                // the interceptor rotates both cookies in the response headers of
                // ANY authenticated request. So we just need to retry the original
                // request - if the refresh cookie is still valid, the backend will
                // refresh the JWT automatically and the retry will succeed.
                if (isRefreshing) return false;
                isRefreshing = true;

                try {
                    const me = await apiGet("/api/users/me");
                    setUser(me);
                    return true;
                } catch {
                    setUser(null);
                    return false;
                } finally {
                    isRefreshing = false;
                }
            },
        });
    }, []);

    const login = async (email, password) => {
        try {
            const res = await apiPost("/api/auth/login", { email, password });
            setUser(res?.user ?? null);
            return true;
        } catch {
            return false;
        }
    };

    const logout = async () => {
        try {
            await apiPost("/api/auth/logout");
        } catch {}
        sessionStorage.removeItem("returnUrl");
        setUser(null);
    };

    const updateUser = (updates) => {
        setUser(prev => prev ? { ...prev, ...updates } : prev);
    };

    return (
        <AuthContext.Provider value={{
            user,
            loading,
            bootstrapped,
            authError,
            login,
            logout,
            updateUser,
            setUser
        }}>
            {children}
        </AuthContext.Provider>
    );
}
