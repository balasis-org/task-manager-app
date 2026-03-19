import { useState, useRef, useEffect } from "react";
import { FiUsers, FiPlus, FiSearch } from "react-icons/fi";
import { useBlobUrl } from "@context/BlobSasContext";
import { formatRole } from "@assets/js/formatLabel";
import MemberDetailPopup from "@components/popups/MemberDetailPopup";
import "@styles/topbar/TopBarMembersDropdown.css";

// members dropdown in the top bar. searchable, shows online/offline dots
// via presenceUserIds set. clicking a member opens MemberDetailPopup.
export default function TopBarMembersDropdown({
    members,
    canInvite,
    onOpenInvite,
    activeGroup,
    isLeader,
    user,
    groupDetail,
    onLeaveGroup,
    presenceUserIds,
}) {
    const blobUrl = useBlobUrl();
    const [membersDropdown, setMembersDropdown] = useState(false);
    const [memberSearch, setMemberSearch] = useState("");
    const [selectedMember, setSelectedMember] = useState(null);
    const membersRef = useRef(null);
    const memberSearchRef = useRef(null);

    const filteredMembers = (members || []).filter((m) => {
        if (!memberSearch.trim()) return true;
        const q = memberSearch.toLowerCase();
        return (
            (m.user?.name || "").toLowerCase().includes(q) ||
            (m.user?.email || "").toLowerCase().includes(q)
        );
    });

    const onlineSet = new Set(presenceUserIds || []);

    useEffect(() => {
        if (membersDropdown && memberSearchRef.current) {
            memberSearchRef.current.focus();
        }
        if (!membersDropdown) {
            setMemberSearch("");
        }
    }, [membersDropdown]);

    // mousedown, not click - so the dropdown closes before the click
    // event reaches whatever the user tapped on next
    useEffect(() => {
        function handleClick(e) {
            if (membersRef.current && !membersRef.current.contains(e.target)) {
                setMembersDropdown(false);
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, []);

    return (
        <>
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
                                    <div
                                        key={m.id}
                                        className="topbar-dropdown-item topbar-member-row topbar-member-clickable"
                                        onClick={() => {
                                            setSelectedMember(m);
                                            setMembersDropdown(false);
                                        }}
                                    >
                                        <span className={`topbar-member-status-dot${onlineSet.has(m.user?.id) ? " online" : ""}`} />
                                        <img
                                            src={(m.user?.imgUrl) ? blobUrl(m.user.imgUrl) : (m.user?.defaultImgUrl)
                                                ? blobUrl(m.user.defaultImgUrl) : ""}
                                            alt=""
                                            className={`topbar-member-img tier-ring-${m.user?.subscriptionPlan || 'FREE'}`}
                                        />
                                        <span className="topbar-member-name">{m.user?.name || m.user?.email}</span>
                                        {m.user?.sameOrg && <span className="topbar-org-badge" title="Same organisation">ORG</span>}
                                        <span className="topbar-member-role">
                                            {formatRole(m.role)}
                                        </span>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                )}
            </div>

            {selectedMember && (
                <MemberDetailPopup
                    member={selectedMember}
                    groupId={activeGroup?.id}
                    isGroupLeader={isLeader}
                    isSelf={selectedMember.user?.id === user?.id}
                    taskPreviews={groupDetail?.tp}
                    onClose={() => setSelectedMember(null)}
                    onRefresh={() => { if (onLeaveGroup) onLeaveGroup("refresh"); }}
                    isOnline={onlineSet.has(selectedMember.user?.id)}
                />
            )}
        </>
    );
}
