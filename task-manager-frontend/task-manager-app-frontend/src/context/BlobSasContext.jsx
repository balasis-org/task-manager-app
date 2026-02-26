import { createContext, useContext, useCallback } from "react";

const BlobUrlContext = createContext(null);

/**
 * All blob containers (images, assets, etc.) are private.  Front Door
 * origin-auth (managed identity) is the sole authentication layer —
 * no SAS tokens needed.  This eliminates SAS polling overhead and
 * preserves Front Door edge caching (SAS query strings bust the cache).
 *
 * In development, VITE_BLOB_BASE points at the local Azurite emulator.
 */
const FD_BLOB_PREFIX = import.meta.env.VITE_FD_BLOB_PREFIX || "";
const BLOB_BASE      = import.meta.env.VITE_BLOB_BASE      || "";
const IMAGE_BASE = FD_BLOB_PREFIX || BLOB_BASE;

export function BlobSasProvider({ children }) {
    const blobUrl = useCallback(
        (path) => (path ? IMAGE_BASE + path : ""),
        []
    );

    return (
        <BlobUrlContext.Provider value={blobUrl}>
            {children}
        </BlobUrlContext.Provider>
    );
}

/**
 * Returns a function `blobUrl(path)` that builds the full blob URL.
 * Front Door origin-auth handles authentication — no SAS needed.
 *
 * ```jsx
 * const blobUrl = useBlobUrl();
 * <img src={blobUrl(user.imgUrl)} />
 * ```
 */
export function useBlobUrl() {
    const ctx = useContext(BlobUrlContext);
    if (ctx === null) {
        return (path) => (path ? IMAGE_BASE + path : "");
    }
    return ctx;
}
