import { useState, useRef, useEffect } from "react";
import { FiSettings, FiUsers, FiPlus, FiLogOut, FiSearch, FiMinus, FiFilter } from "react-icons/fi";
import { apiDelete } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import FilterPanel from "@components/dashboard/FilterPanel";
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
    onLeaveGroup,
    user,
    groupDetail,
    filters,
    isFilterApplied,
    onDraftChange,
    onApplyFilters,
    onEditFilters,
    onFiltersClear,
}) {
    const showToast = useToast();
    const [groupDropdown, setGroupDropdown] = useState(false);
    const [membersDropdown, setMembersDropdown] = useState(false);
    const [memberSearch, setMemberSearch] = useState("");
    const [confirmRemove, setConfirmRemove] = useState(null); // membershipId
    const [confirmLeave, setConfirmLeave] = useState(false);
    const [filterOpen, setFilterOpen] = useState(false);
    const groupRef = useRef(null);
    const membersRef = useRef(null);
    const memberSearchRef = useRef(null);
    const filterRef = useRef(null);

    const canInvite = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";
    const canSettings = myRole === "GROUP_LEADER";
    const canRemoveMembers = myRole === "GROUP_LEADER";
    const isLeader = myRole === "GROUP_LEADER";

    const roleLabel = myRole ? myRole.replace(/_/g, " ").toLowerCase() : null;

    // check for unseen group events
    const myMembership = user && members?.length
        ? members.find((m) => m.user?.id === user.id)
        : null;
    const hasUnseenEvents = (() => {
        const lastEvent = groupDetail?.lged;
        const lastSeen = myMembership?.lastSeenGroupEvents;
        if (!lastEvent) return false;
        if (!lastSeen) return true;
        return new Date(lastEvent) > new Date(lastSeen);
    })();

    const filteredMembers = members.filter((m) => {
        if (!memberSearch.trim()) return true;
        const q = memberSearch.toLowerCase();
        return (
            (m.user?.name || "").toLowerCase().includes(q) ||
            (m.user?.email || "").toLowerCase().includes(q)
        );
    });

    useEffect(() => {
        if (membersDropdown && memberSearchRef.current) {
            memberSearchRef.current.focus();
        }
        if (!membersDropdown) {
            setMemberSearch("");
            setConfirmRemove(null);
        }
    }, [membersDropdown]);

    // click outside closes dropdowns
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

    async function handleRemoveMember(membershipId) {
        try {
            await apiDelete(`/api/groups/${activeGroup.id}/groupMembership/${membershipId}`);
            showToast("Member removed", "success");
            setConfirmRemove(null);
            // Trigger a refresh
            if (onLeaveGroup) onLeaveGroup("refresh");
        } catch (err) {
            showToast(err?.message || "Failed to remove member");
        }
    }

    async function handleLeaveGroup() {
        if (!myMembership) return;
        try {
            await apiDelete(`/api/groups/${activeGroup.id}/groupMembership/${myMembership.id}`);
            showToast("You left the group", "success");
            setConfirmLeave(false);
            if (onLeaveGroup) onLeaveGroup("left");
        } catch (err) {
            // If 403/404 the user was already removed — treat it as "left"
            if (err?.status === 403 || err?.status === 404) {
                showToast("You are no longer a member of this group.", "info");
                setConfirmLeave(false);
                if (onLeaveGroup) onLeaveGroup("left");
            } else {
                showToast(err?.message || "Failed to leave group");
            }
        }
    }

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
                        {roleLabel && (
                            <span className="topbar-role-tag">({roleLabel})</span>
                        )}
                        {/* leave btn (not for leader) */}
                        {!isLeader && activeGroup && (
                            <>
                                {!confirmLeave ? (
                                    <button
                                        className="topbar-icon-btn topbar-leave-btn"
                                        onClick={() => setConfirmLeave(true)}
                                        title="Leave group"
                                    >
                                        <FiLogOut size={14} />
                                    </button>
                                ) : (
                                    <span className="topbar-confirm-inline">
                                        <span className="topbar-confirm-text">Leave group?</span>
                                        <button className="topbar-confirm-yes" onClick={handleLeaveGroup}>Yes</button>
                                        <button className="topbar-confirm-no" onClick={() => setConfirmLeave(false)}>No</button>
                                    </span>
                                )}
                            </>
                        )}
                    </div>

                    <div className="topbar-right">
                        <div className="topbar-dropdown-wrapper" ref={filterRef}>
                            <button
                                className={`topbar-icon-btn topbar-filter-btn${isFilterApplied ? " filter-active" : ""}`}
                                onClick={() => setFilterOpen((v) => !v)}
                                title="Filter tasks"
                            >
                                <FiFilter size={15} />
                                {isFilterApplied && <span className="topbar-filter-dot" />}
                            </button>
                            {filterOpen && (
                                <FilterPanel
                                    members={members}
                                    filters={filters}
                                    isApplied={isFilterApplied}
                                    onDraftChange={onDraftChange}
                                    onApply={onApplyFilters}
                                    onEdit={onEditFilters}
                                    onClear={onFiltersClear}
                                    onClose={() => setFilterOpen(false)}
                                />
                            )}
                        </div>

                        <button className="topbar-icon-btn topbar-events-btn" onClick={onOpenGroupEvents} title="Group Events">
                            📋
                            {hasUnseenEvents && (
                                <>
                                    <span className="topbar-notif-badge">NEW</span>
                                </>
                            )}
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
                                <div className="topbar-dropdown topbar-members-dropdown">
                                    <div className="topbar-member-search">
                                        <FiSearch size={13} className="topbar-member-search-icon" />
                                        <input
                                            ref={memberSearchRef}
                                            type="text"
                                            value={memberSearch}
                                            onChange={(e) => setMemberSearch(e.target.value)}
                                            placeholder="Search members…"
                                            className="topbar-member-search-input"
                                        />
                                    </div>
                                    <div className="topbar-members-list">
                                        {filteredMembers.length === 0 ? (
                                            <div className="topbar-dropdown-item muted">No members found</div>
                                        ) : (
                                            filteredMembers.map((m) => (
                                                <div key={m.id} className="topbar-dropdown-item topbar-member-row">
                                                    <img
                                                        src={ (m.user?.imgUrl) ? blobBase+m.user.imgUrl : (m.user?.defaultImgUrl)
                                                            ? blobBase + m.user.defaultImgUrl : ""}
                                                        alt=""
                                                        className="topbar-member-img"
                                                    />
                                                    <span className="topbar-member-name">{m.user?.name || m.user?.email}</span>
                                                    {m.user?.sameOrg && <span className="topbar-org-badge" title="Same organisation">ORG</span>}
                                                    <span className="topbar-member-role">
                                                        {m.role}
                                                    </span>
                                                    {/* remove btn */}
                                                    {canRemoveMembers && m.user?.id !== user?.id && (
                                                        confirmRemove === m.id ? (
                                                            <span className="topbar-member-confirm">
                                                                <button className="topbar-confirm-yes" onClick={() => handleRemoveMember(m.id)} title="Confirm remove">✓</button>
                                                                <button className="topbar-confirm-no" onClick={() => setConfirmRemove(null)} title="Cancel">✕</button>
                                                            </span>
                                                        ) : (
                                                            <button
                                                                className="topbar-member-remove"
                                                                onClick={(e) => { e.stopPropagation(); setConfirmRemove(m.id); }}
                                                                title="Remove from group"
                                                            >
                                                                <FiMinus size={12} />
                                                            </button>
                                                        )
                                                    )}
                                                </div>
                                            ))
                                        )}
                                    </div>
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
                                                    src={(g.imgUrl)? blobBase+g.imgUrl : blobBase + g.defaultImgUrl}
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
                    </div>
                </div>
            )}

            <button className="topbar-toggle" onClick={onToggle} title={open ? "Hide top bar" : "Show top bar"}>
                {open ? "▲" : "▼"}
            </button></div>
)
}

