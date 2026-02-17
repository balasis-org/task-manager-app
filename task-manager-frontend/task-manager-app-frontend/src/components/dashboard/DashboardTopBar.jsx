import { useState, useRef, useEffect } from "react";
import { FiSettings, FiUsers, FiPlus } from "react-icons/fi";
import "@styles/dashboard/DashboardTopBar.css";
import blobBase from "@blobBase";

export default function DashboardTopBar({
    groups,
    activeGroup,
    members,
    myRole,
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

    const canInvite = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";
    const canSettings = myRole === "GROUP_LEADER";

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
            {open && (
                <div className="topbar-content">
                    <div className="topbar-left">
                        {canSettings && (
                            <button className="topbar-icon-btn" onClick={onOpenGroupSettings} title="Group Settings">
                                <FiSettings size={16} />
                            </button>
                        )}
                    </div>

                    <div className="topbar-right">
                        <button className="topbar-icon-btn" onClick={onOpenGroupEvents} title="Group Events">
                            📋
                        </button>

                        <div className="topbar-dropdown-wrapper topbar-combo" ref={membersRef}>
                            <button
                                className="topbar-dropdown-btn topbar-members-btn"
                                onClick={() => setMembersDropdown((v) => !v)}
                            >
                                <FiUsers size={16} />
                                <span>Members</span>
                                <span className="caret">▾</span>
                            </button>
                            {canInvite && (
                                <button className="topbar-plus-combo" onClick={onOpenInvite} title="Invite to group">
                                    <FiPlus size={14} />
                                </button>
                            )}

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

                        <div className="topbar-dropdown-wrapper topbar-combo" ref={groupRef}>
                            <button
                                className="topbar-dropdown-btn topbar-group-btn"
                                onClick={() => setGroupDropdown((v) => !v)}
                            >
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
                                <span className="topbar-group-name">{activeGroup?.name || "Select group"}</span>
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
                </div>
            )}

            <button className="topbar-toggle" onClick={onToggle} title={open ? "Hide top bar" : "Show top bar"}>
                {open ? "▲" : "▼"}
            </button>
        </div>
    );
}
