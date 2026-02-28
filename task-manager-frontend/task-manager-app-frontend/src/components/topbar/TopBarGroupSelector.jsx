import { useState, useRef, useEffect } from "react";
import { FiPlus } from "react-icons/fi";
import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/topbar/TopBarGroupSelector.css";

export default function TopBarGroupSelector({
    groups,
    activeGroup,
    onSelectGroup,
    onOpenNewGroup,
}) {
    const blobUrl = useBlobUrl();
    const imgSrc = (g) => blobUrl(g.imgUrl || g.defaultImgUrl);
    const [groupDropdown, setGroupDropdown] = useState(false);
    const groupRef = useRef(null);

    useEffect(() => {
        function handleClick(e) {
            if (groupRef.current && !groupRef.current.contains(e.target)) {
                setGroupDropdown(false);
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, []);

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
            <button className="topbar-plus-combo" onClick={onOpenNewGroup} title="New group">
                <FiPlus size={14} />
            </button>

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
        </div>
    );
}
