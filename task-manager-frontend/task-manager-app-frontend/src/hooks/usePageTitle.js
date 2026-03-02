import { useEffect } from "react";

const BRAND = "myteamtasks";

/**
 * Sets `document.title` to  "page — myteamtasks"  (or just the brand if no page given).
 * Restores the brand-only title on unmount so navigations that forget to call the
 * hook still show something reasonable.
 */
export default function usePageTitle(page) {
    useEffect(() => {
        document.title = page ? `${page} — ${BRAND}` : BRAND;
        return () => { document.title = BRAND; };
    }, [page]);
}
