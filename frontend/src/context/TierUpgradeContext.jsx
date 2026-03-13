import { createContext, useContext, useState, useCallback } from "react";
import TierUpgradePopup from "@components/popups/TierUpgradePopup";

const TierUpgradeContext = createContext(() => {});

export function useTierUpgrade() {
    return useContext(TierUpgradeContext);
}

export default function TierUpgradeProvider({ children }) {
    const [open, setOpen] = useState(false);

    const openTierUpgrade = useCallback(() => setOpen(true), []);

    return (
        <TierUpgradeContext.Provider value={openTierUpgrade}>
            {children}
            {open && <TierUpgradePopup onClose={() => setOpen(false)} />}
        </TierUpgradeContext.Provider>
    );
}
