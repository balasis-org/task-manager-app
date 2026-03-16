import { useState, useRef, useEffect } from "react";
import { FiPlus } from "react-icons/fi";
import { useBlobUrl } from "@context/BlobSasContext";
import { useTierUpgrade } from "@context/TierUpgradeContext";
import "@styles/topbar/TopBarGroupSelector.css";
import "@styles/popups/Popup.css";

export default function TopBarGroupSelector({
    groups,
    activeGroup,
    onSelectGroup,
    onOpenNewGroup,
    user,
}) {
    const blobUrl = useBlobUrl();
    const openTierUpgrade = useTierUpgrade();
    const imgSrc = (g) => blobUrl(g.imgUrl || g.defaultImgUrl);
    const [groupDropdown, setGroupDropdown] = useState(false);
    const [showLimitMsg, setShowLimitMsg] = useState(false);
    const groupRef = useRef(null);

    const maxGroups = user?.maxGroups ?? Infinity;
    const atCap = groups.length >= maxGroups;
    const isMaxTier = user?.subscriptionPlan === "TEAMS_PRO";

    useEffect(() => {
        function handleClick(e) {
            if (groupRef.current && !groupRef.current.contains(e.target)) {
                setGroupDropdown(false);
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, []);

    function handlePlusClick() {
        if (atCap && !isMaxTier) {
            setShowLimitMsg(true);
        } else {
            onOpenNewGroup();
        }
    }

    return (
        <div className="topbar-dropdown-wrapper topbar-combo" ref={groupRef}>
            <button
                className="topbar-dropdown-btn topbar-group-btn"
                onClick={() => setGroupDropdown((v) => !v)}
            >
                <span className="topbar-group-img-wrapper">
                    {activeGroup?.imgUrl || activeGroup?.defaultImgUrl ? (
                        <img
                            src={imgSrc(activeGroup)}
                            alt=""
                            className="topbar-group-img"
                        />
                    ) : (
                        <span className="topbar-group-img placeholder" />
                    )}
                </span>
                <span className="topbar-group-name" title={activeGroup?.name || "Select group"}>{activeGroup?.name || "Select group"}</span>
                <span className="caret">▾</span>
            </button>
            {/* Hide + only at max tier when cap reached; otherwise always show */}
            {!(atCap && isMaxTier) && (
                <button className="topbar-plus-combo" onClick={handlePlusClick} title="New group">
                    <FiPlus size={14} />
                </button>
            )}

            {groupDropdown && (
                <div className="topbar-dropdown">
                    {groups.map((g) => (
                        <div
                            key={g.id}
                            className={`topbar-dropdown-item${
                                g.id === activeGroup?.id ? " active" : ""
                            }`}
                            onClick={() => {
                                onSelectGroup(g);
                                setGroupDropdown(false);
                            }}
                        >
                            {g.imgUrl || g.defaultImgUrl ? (
                                <img
                                    src={imgSrc(g)}
                                    alt=""
                                    className="topbar-group-img-small"
                                />
                            ) : (
                                <span className="topbar-group-img-small placeholder" />
                            )}
                            <span className="topbar-dropdown-group-name" title={g.name}>{g.name}</span>
                        </div>
                    ))}
                </div>
            )}

            {showLimitMsg && (
                <div className="popup-overlay" onMouseDown={() => setShowLimitMsg(false)}>
                    <div className="popup-card group-limit-card" onMouseDown={(e) => e.stopPropagation()}>
                        <p className="group-limit-text">
                            You&rsquo;ve reached the maximum number of groups
                            for your current plan ({groups.length}/{maxGroups}).
                        </p>
                        <p className="group-limit-text">
                            Explore your{" "}
                            <span
                                className="group-limit-plans-link"
                                role="button"
                                tabIndex={0}
                                onClick={() => { setShowLimitMsg(false); openTierUpgrade(); }}
                                onKeyDown={(e) => { if (e.key === "Enter") { setShowLimitMsg(false); openTierUpgrade(); } }}
                            >
                                plans
                            </span>{" "}
                            for higher limits.
                        </p>
                        <div className="group-limit-actions">
                            <button className="btn-secondary" onClick={() => setShowLimitMsg(false)}>
                                OK
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
