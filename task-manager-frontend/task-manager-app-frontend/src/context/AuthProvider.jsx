import { useEffect, useState } from "react";
import { AuthContext } from "./AuthContext";
import { apiGet, apiPost, registerAuthHandlers } from "@assets/js/apiClient.js";

let isRefreshing = false;

export default function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);        // network activity
    const [bootstrapped, setBootstrapped] = useState(false); // FIRST LOAD ONLY

    useEffect(() => {
        const loadUser = async () => {
            try {
                const me = await apiGet("/api/auth/me");
                setUser(me?.user ?? null);
            } catch {
                setUser(null);
            } finally {
                setLoading(false);
                setBootstrapped(true); // ✅ first load completed
            }
        };

        loadUser();

        registerAuthHandlers({
            onUnauthorized: async (config) => {
                if (
                    config?.url?.startsWith("/api/auth") ||
                    config?.url === "/api/auth/me"
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

            onForbidden: () => setUser(null),
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
            bootstrapped, // 👈 important
            login,
            logout,
            updateUser,
            setUser
        }}>
            {children}
        </AuthContext.Provider>
    );
}
