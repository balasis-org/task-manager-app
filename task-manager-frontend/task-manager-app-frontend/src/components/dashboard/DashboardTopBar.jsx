import { useState, useRef } from "react";
import { FiSettings, FiLogOut, FiFilter } from "react-icons/fi";
import { apiDelete } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import FilterPanel from "@components/dashboard/FilterPanel";
import TopBarMembersDropdown from "@components/topbar/TopBarMembersDropdown";
import TopBarGroupSelector from "@components/topbar/TopBarGroupSelector";
import "@styles/dashboard/DashboardTopBar.css";

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
    const [confirmLeave, setConfirmLeave] = useState(false);
    const [filterOpen, setFilterOpen] = useState(false);
    const filterRef = useRef(null);

    const canInvite = myRole === "GROUP_LEADER" || myRole === "TASK_MANAGER";
    const canSettings = myRole === "GROUP_LEADER";
    const isLeader = myRole === "GROUP_LEADER";

    const roleLabel = myRole ? myRole.replace(/_/g, " ").toLowerCase() : null;

    const myMembership = user && members?.length
        ? members.find((m) => m.user?.id === user.id)
        : null;
    // lged = "last group event date", tp = "task previews"
    // (short keys from the backend list-groups DTO to keep payloads small)
    const hasUnseenEvents = (() => {
        const lastEvent = groupDetail?.lged;
        const lastSeen = myMembership?.lastSeenGroupEvents;
        if (!lastEvent) return false;
        if (!lastSeen) return true;
        return new Date(lastEvent) > new Date(lastSeen);
    })();

    async function handleLeaveGroup() {
        if (!myMembership) return;
        try {
            await apiDelete(`/api/groups/${activeGroup.id}/groupMembership/${myMembership.id}`);
            showToast("You left the group", "success");
            setConfirmLeave(false);
            if (onLeaveGroup) onLeaveGroup("left");
        } catch (err) {
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

                        <TopBarMembersDropdown
                            members={members}
                            canInvite={canInvite}
                            onOpenInvite={onOpenInvite}
                            activeGroup={activeGroup}
                            isLeader={isLeader}
                            user={user}
                            groupDetail={groupDetail}
                            onLeaveGroup={onLeaveGroup}
                        />

                        <TopBarGroupSelector
                            groups={groups}
                            activeGroup={activeGroup}
                            onSelectGroup={onSelectGroup}
                            onOpenNewGroup={onOpenNewGroup}
                        />
                    </div>
                </div>
            )}

            <button className="topbar-toggle" onClick={onToggle} title={open ? "Hide top bar" : "Show top bar"}>
                {open ? "▲" : "▼"}
            </button>
        </div>
    );
}
