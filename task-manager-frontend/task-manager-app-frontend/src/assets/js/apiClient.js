import API_BASE from "@apiBase";

let authHandlers = {
    onUnauthorized: null,
    onForbidden: null,
};

export function registerAuthHandlers(handlers) {
    authHandlers = { ...authHandlers, ...handlers };
}

export const apiGet = (path, options) =>
    apiRequest(path, { method: "GET", ...options});

export const apiPost = (path, body, options = {}) =>
    apiRequest(path, { method: "POST", body, ...options});

export const apiPatch = (path, body, options = {}) =>
    apiRequest(path, { method: "PATCH", body, ...options});

export const apiPut = (path, body, options = {}) =>
    apiRequest(path, { method: "PUT", body, ...options});

export const apiDelete = (path, body, options = {}) =>
    apiRequest(path, { method: "DELETE", body, ...options});

async function apiRequest(path, options = {}, retry = true) {
    const method = options.method || "GET";
    const isFormData = options.body instanceof FormData;

    const headers = { ...(options.headers || {}) };

    if (["POST", "PUT", "PATCH", "DELETE"].includes(method) && !isFormData) {
        headers["Content-Type"] = "application/json";
    }

    const fetchOptions = {
        method,
        headers,
        credentials: "include",
    };

    // Handle body: FormData goes raw, objects get stringified
    if (options.body) {
        if (isFormData) {
            fetchOptions.body = options.body;
        } else if (typeof options.body !== "string") {
            fetchOptions.body = JSON.stringify(options.body);
        } else {
            fetchOptions.body = options.body;
        }
    }

    if (options.signal) fetchOptions.signal = options.signal;

    let res;
    try {
        res = await fetch(`${API_BASE}${path}`, fetchOptions);
    } catch {
        throw { status: 0, message: "Network error" };
    }

    /**
     * HTTP-level auth errors
     */
    if (res.status === 401 || res.status === 403) {
        return handleAuthStatus(res.status, path, options, retry);
    }

    /**
     * Other HTTP errors
     */
    if (!res.ok) {
        let body = null;
        try { body = await res.text(); } catch {}
        throw {
            status: res.status,
            message: body || "HTTP error",
        };
    }

    // Blob responses (file downloads)
    if (options.responseType === "blob") {
        return res.blob();
    }

    let json = null;
    try { json = await res.json(); } catch {}
    return json;
}


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
    // HTTP auth errors
    if (res.status === 401 || res.status === 403) {
        return handleAuthStatus(res.status, path, fetchOptions, true);
    }

    if (!res.ok) {
        // json was already parsed above; use it for the error message
        const errMsg = (json && typeof json === "object") ? (json.message || json.error || JSON.stringify(json))
                     : (json && typeof json === "string") ? json
                     : "HTTP error";
        throw { status: res.status, message: errMsg };
    }

    // Return raw JSON body (no wrapper unwrapping)
    return json;
}

async function handleAuthStatus(status, path, options, retry) {
    // 401 → try refresh once
    if (status === 401 && retry && authHandlers.onUnauthorized) {
        // Provide request context so handlers can decide (e.g. skip /api/auth/*)
        const shouldRetry = await authHandlers.onUnauthorized({ url: path, options });
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
        message: status === 401 ? "Unauthorized" : status === 403 ? "Forbidden" : "Auth error",
    };
}




