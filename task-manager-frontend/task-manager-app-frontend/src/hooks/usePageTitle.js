import { useEffect } from "react";

const BRAND = "myteamtasks";

// sets document.title and restores the brand name on unmount
export default function usePageTitle(page) {
    useEffect(() => {
        document.title = page ? `${page} - ${BRAND}` : BRAND;
        return () => { document.title = BRAND; };
    }, [page]);
}
