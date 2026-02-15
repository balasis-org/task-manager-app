import API_BASE from "@apiBase";

let authHandlers = {
    onUnauthorized: null,
    onForbidden: null,
};

export function registerAuthHandlers(handlers) {
    authHandlers = { ...authHandlers, ...handlers };
}

async function handleAuthStatus(status, path, options, retry) {
    // 401 → try refresh once
    if (status === 401 && retry && authHandlers.onUnauthorized) {
        const shouldRetry = await authHandlers.onUnauthorized();
        if (shouldRetry) {
            return apiRequest(path, options, false);
        }
    }

    // 403 → forbidden
    if (status === 403) {
        authHandlers.onForbidden?.();
    }

    throw {
        status,
        message:
            status === 401
                ? "Unauthorized"
                : status === 403
                    ? "Forbidden"
                    : "Auth error",
    };
}

async function apiRequest(path, options = {}, retry = true) {
    const method = options.method || "GET";

    const headers = { ...(options.headers || {}) };
    if (["POST", "PUT", "PATCH","DELETE"].includes(method)) {
        headers["Content-Type"] = "application/json";
    }

    const fetchOptions = {
        method,
        headers,
        credentials: "include",
        ...options,
    };

    // Only stringify body if provided and method supports it
    if (options.body && typeof options.body !== "string") {
        fetchOptions.body = JSON.stringify(options.body);
    }

    if (options.signal) fetchOptions.signal = options.signal;

    let res;
    try {
        res = await fetch(`${API_BASE}${path}`, fetchOptions);
    } catch {
        throw { status: 0, message: "Network error" };
    }

    let json = null;
    try {
        json = await res.json();
    } catch {
        // no body (rare in your API)
    }

    /**
     *  API-level errors FIRST (even if HTTP 200)
     */
    if (json?.apiError) {
        const status = json.apiError.status;

        if (status === 401 || status === 403) {
            return handleAuthStatus(status, path, options, retry);
        }

        throw {
            status,
            message: json.apiError.message ?? "API error",
        };
    }

    /**
     * HTTP-level auth errors (fallback)
     */
    if (res.status === 401 || res.status === 403) {
        return handleAuthStatus(res.status, path, options, retry);
    }

    /**
     * Other HTTP errors
     */
    if (!res.ok) {
        throw {
            status: res.status,
            message: "HTTP error",
        };
    }

    /**
     * Success
     */
    return json?.data ?? json;
}

/* =====================
   Public API helpers
   ===================== */

export const apiGet = (path, options) =>
    apiRequest(path, { method: "GET", ...options });

export const apiPost = (path, body, options = {}) =>
    apiRequest(path, {
        method: "POST",
        body: JSON.stringify(body),
        ...options,
    });

export const apiPut = (path, body, options = {}) =>
    apiRequest(path, {
        method: "PUT",
        body: JSON.stringify(body),
        ...options,
    });

export const apiPatch = (path, body, options = {}) =>
    apiRequest(path, {
        method: "PATCH",
        body: JSON.stringify(body),
        ...options,
    });

export const apiDelete = (path, body, options = {}) =>
    apiRequest(path, { method: "DELETE", body, ...options });





/**
 * Multipart/form-data request helper
 * Automatically keeps credentials and supports file uploads
 */
export async function apiMultipart(path, formData, options = {}) {
    const fetchOptions = {
        method: options.method || "POST",
        body: formData,
        credentials: "include", // keep cookies
        headers: {
            ...(options.headers || {}),
            // NOTE: Do NOT set Content-Type! The browser will set multipart boundary automatically
        },
        ...options,
    };

    let res;
    try {
        res = await fetch(`${API_BASE}${path}`, fetchOptions);
    } catch {
        throw { status: 0, message: "Network error" };
    }

    let json = null;
    try {
        json = await res.json();
    } catch {
        // no body (rare)
    }

    // Handle API-level errors first
    if (json?.apiError) {
        const status = json.apiError.status;

        if (status === 401 || status === 403) {
            return handleAuthStatus(status, path, fetchOptions, true);
        }

        throw {
            status,
            message: json.apiError.message ?? "API error",
        };
    }

    // HTTP auth errors fallback
    if (res.status === 401 || res.status === 403) {
        return handleAuthStatus(res.status, path, fetchOptions, true);
    }

    if (!res.ok) {
        throw { status: res.status, message: "HTTP error" };
    }

    return json?.data ?? json;
}
