import { createContext, useContext, useCallback } from "react";

// builds full image URLs by prepending the Front Door blob prefix or direct
// blob base URL. the frontend stores only relative paths (e.g. "profile-images/abc.jpg")
// and this context adds the CDN/storage prefix at render time.
const BlobUrlContext = createContext(null);

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

export function useBlobUrl() {
    const ctx = useContext(BlobUrlContext);
    if (ctx === null) {
        return (path) => (path ? IMAGE_BASE + path : "");
    }
    return ctx;
}
