import API_BASE from "@apiBase";

let authHandlers = {
    onUnauthorized: null,
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

    // 401 — try token refresh once
    if (res.status === 401) {
        return handleAuthStatus(res.status, path, options, retry);
    }

    // any other non-ok (including 403) → surface to caller as an error
    if (!res.ok) {
        let body = null;
        try { body = await res.text(); } catch {}

        // The backend may return plain text OR a JSON object with "message" / "error".
        // Try to extract the human-readable message from either format.
        let message = body || "HTTP error";
        if (body) {
            try {
                const parsed = JSON.parse(body);
                if (typeof parsed === "object" && parsed !== null) {
                    message = parsed.message || parsed.error || body;
                }
            } catch { /* not JSON — keep raw text */ }
        }
        throw {
            status: res.status,
            message,
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


// multipart/form-data helper (file uploads etc)
export async function apiMultipart(path, formData, options = {}) {
    const fetchOptions = {
        method: options.method || "POST",
        body: formData,
        credentials: "include", // keep cookies
        headers: {
            ...(options.headers || {}),
            // dont set Content-Type, browser adds multipart boundary automatically
        },
        ...options,
    };

    let res;
    try {
        res = await fetch(`${API_BASE}${path}`, fetchOptions);
    } catch {
        throw { status: 0, message: "Network error" };
    }

    // HTTP auth errors — only 401 triggers auth flow
    if (res.status === 401) {
        return handleAuthStatus(res.status, path, fetchOptions, true);
    }

    // Read the body as text first, then attempt JSON parse.
    // The backend returns plain-text error messages for most exceptions,
    // so res.json() would fail and we'd lose the real message.
    let raw = null;
    try { raw = await res.text(); } catch { /* empty body */ }

    if (!res.ok) {
        let errMsg = "HTTP error";
        if (raw) {
            try {
                const parsed = JSON.parse(raw);
                errMsg = (typeof parsed === "object")
                    ? (parsed.message || parsed.error || JSON.stringify(parsed))
                    : String(parsed);
            } catch {
                // not JSON — use the raw text as-is (backend plain-text error)
                errMsg = raw;
            }
        }
        throw { status: res.status, message: errMsg };
    }

    // Return parsed JSON body
    let json = null;
    if (raw) {
        try { json = JSON.parse(raw); } catch { /* non-JSON success body */ }
    }
    return json;
}

async function handleAuthStatus(status, path, options, retry) {
    // 401 - try refresh once
    if (status === 401 && retry && authHandlers.onUnauthorized) {
        const shouldRetry = await authHandlers.onUnauthorized({ url: path, options });
        if (shouldRetry) {
            return apiRequest(path, options, false);
        }
    }

    throw {
        status,
        message: "Unauthorized",
    };
}




