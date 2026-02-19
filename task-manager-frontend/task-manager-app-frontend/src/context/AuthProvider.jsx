import { useEffect, useState } from "react";
import { AuthContext } from "./AuthContext";
import { apiGet, apiPost, registerAuthHandlers } from "@assets/js/apiClient.js";

let isRefreshing = false;

export default function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [bootstrapped, setBootstrapped] = useState(false);

    useEffect(() => {
        const loadUser = async () => {
            try {
                const me = await apiGet("/api/users/me");
                setUser(me);
            } catch {
                setUser(null);
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

                if (isRefreshing) return false;
                isRefreshing = true;

                try {
                    await apiPost("/api/auth/refresh");
                    const me = await apiGet("/api/auth/me");
                    setUser(me?.user ?? null);
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
            login,
            logout,
            updateUser,
            setUser
        }}>
            {children}
        </AuthContext.Provider>
    );
}
