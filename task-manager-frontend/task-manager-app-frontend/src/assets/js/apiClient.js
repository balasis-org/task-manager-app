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
        res = await fetch(path, fetchOptions);
    } catch {
        throw { status: 0, message: "Network error" };
    }

    if (res.status === 401) {
        return handleAuthStatus(res.status, path, options, retry);
    }

    if (res.status === 429) {
        const retryAfter = res.headers.get("Retry-After");
        let body = null;
        try { body = await res.text(); } catch {}
        throw {
            status: 429,
            message: body || "Too many requests. Please slow down.",
            retryAfter: retryAfter ? parseInt(retryAfter, 10) : null,
        };
    }

    if (res.status === 503) {
        throw {
            status: 503,
            message: "We are sorry but currently we are under heavy traffic. We cannot accept your request at this time, please try again later.",
        };
    }

    if (!res.ok) {
        let body = null;
        try { body = await res.text(); } catch {}

        let message = body || "HTTP error";
        if (body) {
            try {
                const parsed = JSON.parse(body);
                if (typeof parsed === "object" && parsed !== null) {
                    message = parsed.message || parsed.error || body;
                }
            } catch { }
        }
        throw {
            status: res.status,
            message,
        };
    }

    if (options.responseType === "blob") {
        return res.blob();
    }

    let json = null;
    try { json = await res.json(); } catch {}
    return json;
}

export async function apiMultipart(path, formData, options = {}) {
    const fetchOptions = {
        method: options.method || "POST",
        body: formData,
        credentials: "include",
        headers: {
            ...(options.headers || {}),
        },
        ...options,
    };

    let res;
    try {
        res = await fetch(path, fetchOptions);
    } catch {
        throw { status: 0, message: "Network error" };
    }

    if (res.status === 401) {
        return handleAuthStatus(res.status, path, fetchOptions, true);
    }

    if (res.status === 429) {
        const retryAfter = res.headers.get("Retry-After");
        let raw = null;
        try { raw = await res.text(); } catch {}
        throw {
            status: 429,
            message: raw || "Too many requests. Please slow down.",
            retryAfter: retryAfter ? parseInt(retryAfter, 10) : null,
        };
    }

    if (res.status === 503) {
        throw {
            status: 503,
            message: "We are sorry but currently we are under heavy traffic. We cannot accept your request at this time, please try again later.",
        };
    }

    let raw = null;
    try { raw = await res.text(); } catch { }

    if (!res.ok) {
        let errMsg = "HTTP error";
        if (raw) {
            try {
                const parsed = JSON.parse(raw);
                errMsg = (typeof parsed === "object")
                    ? (parsed.message || parsed.error || JSON.stringify(parsed))
                    : String(parsed);
            } catch {

                errMsg = raw;
            }
        }
        throw { status: res.status, message: errMsg };
    }

    let json = null;
    if (raw) {
        try { json = JSON.parse(raw); } catch { }
    }
    return json;
}

async function handleAuthStatus(status, path, options, retry) {

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
