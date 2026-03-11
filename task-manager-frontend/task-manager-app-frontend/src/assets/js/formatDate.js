// formatDate     -> "Feb 28, 2026"
// formatDateTime -> "Feb 28, 2026, 02:30 PM"

export function formatDate(iso, fallback = "") {
    if (!iso) return fallback;
    return new Date(iso).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
    });
}

export function formatDateTime(iso, fallback = "") {
    if (!iso) return fallback;
    return new Date(iso).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}
