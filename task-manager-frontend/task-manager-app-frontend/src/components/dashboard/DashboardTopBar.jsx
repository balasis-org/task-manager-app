import { useState, useRef, useEffect } from "react";
import "@styles/dashboard/DashboardTopBar.css";
import blobBase from "@blobBase";

export default function DashboardTopBar({
    groups,
    activeGroup,
    members,
    open,
    onToggle,
    onSelectGroup,
    onOpenNewGroup,
    onOpenInvite,
    onOpenGroupSettings,
    onOpenGroupEvents,
}) {
    const [groupDropdown, setGroupDropdown] = useState(false);
    const [membersDropdown, setMembersDropdown] = useState(false);
    const groupRef = useRef(null);
    const membersRef = useRef(null);

    // Close dropdowns on outside click
    useEffect(() => {
        function handleClick(e) {
            if (groupRef.current && !groupRef.current.contains(e.target)) {
                setGroupDropdown(false);
            }
            if (membersRef.current && !membersRef.current.contains(e.target)) {
                setMembersDropdown(false);
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, []);

    return (
        <div className={`topbar${open ? "" : " topbar-collapsed"}`}>
            <button className="topbar-toggle" onClick={onToggle} title={open ? "Hide top bar" : "Show top bar"}>
                {open ? "▲" : "▼"}
            </button>

            {open && (
                <div className="topbar-content">
                    {/* Group Settings */}
                    <button className="topbar-icon-btn" onClick={onOpenGroupSettings} title="Group Settings">
                        ⚙
                    </button>

                    {/* Group Events */}
                    <button className="topbar-icon-btn" onClick={onOpenGroupEvents} title="Group Events">
                        📋
                    </button>

                    {/* Members dropdown */}
                    <div className="topbar-dropdown-wrapper" ref={membersRef}>
                        <button
                            className="topbar-dropdown-btn"
                            onClick={() => setMembersDropdown((v) => !v)}
                        >
                            👤 Members ▾
                        </button>
                        <button className="topbar-icon-btn" onClick={onOpenInvite} title="Invite to group">
                            ⊕
                        </button>

                        {membersDropdown && (
                            <div className="topbar-dropdown">
                                {members.length === 0 ? (
                                    <div className="topbar-dropdown-item muted">No members</div>
                                ) : (
                                    members.map((m) => (
                                        <div key={m.id} className="topbar-dropdown-item">
                                            <img
                                                src={ (m.user?.imgUrl) ? blobBase+m.user.imgUrl : (m.user?.defaultImgUrl)
                                                    ? blobBase + m.user.defaultImgUrl : ""}
                                                alt=""
                                                className="topbar-member-img"
                                            />
                                            <span>{m.user?.name || m.user?.email}</span>
                                            <span className="topbar-member-role">
                                                {m.role}
                                            </span>
                                        </div>
                                    ))
                                )}
                            </div>
                        )}
                    </div>

                    {/* Group dropdown */}
                    <div className="topbar-dropdown-wrapper" ref={groupRef}>
                        <span className="topbar-group-img-wrapper">
                            {activeGroup?.imgUrl || activeGroup?.defaultImgUrl ? (
                                <img
                                    src={(activeGroup.imgUrl) ? blobBase + activeGroup.imgUrl : blobBase+ activeGroup.defaultImgUrl}
                                    alt=""
                                    className="topbar-group-img"
                                />
                            ) : (
                                <span className="topbar-group-img placeholder" />
                            )}
                        </span>
                        <button
                            className="topbar-dropdown-btn topbar-group-btn"
                            onClick={() => setGroupDropdown((v) => !v)}
                        >
                            {activeGroup?.name || "Select group"} ▾
                        </button>
                        <button className="topbar-icon-btn" onClick={onOpenNewGroup} title="New group">
                            ⊕
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
                                                src={(g.imgUrl)? blobBase+g.imgUrl : blobBase + g.defaultImgUrl}
                                                alt=""
                                                className="topbar-group-img-small"
                                            />
                                        ) : (
                                            <span className="topbar-group-img-small placeholder" />
                                        )}
                                        <span>{g.name}</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
