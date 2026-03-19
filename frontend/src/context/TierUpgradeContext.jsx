import { createContext, useContext, useState, useCallback } from "react";
import TierUpgradePopup from "@components/popups/TierUpgradePopup";

// provides openTierUpgrade() to any component that hits a plan limit —
// renders TierUpgradePopup globally so it can be triggered from anywhere.
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
