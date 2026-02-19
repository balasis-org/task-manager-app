const CDN_BASE  = import.meta.env.VITE_CDN_BASE  || '';
const BLOB_BASE = import.meta.env.VITE_BLOB_BASE || '';

// Use CDN for public image sources when available, otherwise fall back to blob storage
const IMAGE_BASE = CDN_BASE || BLOB_BASE;

export default IMAGE_BASE;
