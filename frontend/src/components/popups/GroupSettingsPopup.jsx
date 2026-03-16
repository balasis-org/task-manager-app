import { FiAlignLeft, FiMessageSquare, FiMail, FiDownloadCloud, FiShield } from "react-icons/fi";
import { LIMITS } from "@assets/js/inputValidation";
import GsEditableField from "@components/groupsettings/GsEditableField";
import GsToggleField from "@components/groupsettings/GsToggleField";
import GsImageSection from "@components/groupsettings/GsImageSection";
import GsDeleteSection from "@components/groupsettings/GsDeleteSection";
import "@styles/popups/Popup.css";
import "@styles/popups/GroupSettingsPopup.css";

export default function GroupSettingsPopup({ group, groupDetail, members, user, onClose, onUpdated, onDeleted }) {
    const emailDisabled = groupDetail?.op === "FREE" || groupDetail?.op === "STUDENT";

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card popup-card-wide gs-popup" onClick={e => e.stopPropagation()}>
                <h2 title={group.name} className="popup-heading-ellipsis">
                    {group.name} - Settings
                </h2>

                <GsEditableField
                    label="Description"
                    icon={<FiAlignLeft size={14} />}
                    groupId={group.id}
                    fieldKey="description"
                    initialValue={groupDetail?.d || ""}
                    emptyText="No description"
                    onUpdated={onUpdated}
                    maxLength={LIMITS.GROUP_DESCRIPTION}
                    rows={3}
                />

                <GsEditableField
                    label="Announcement"
                    icon={<FiMessageSquare size={14} />}
                    groupId={group.id}
                    fieldKey="announcement"
                    initialValue={groupDetail?.an ?? ""}
                    emptyText="No announcement"
                    onUpdated={onUpdated}
                    maxLength={LIMITS.GROUP_ANNOUNCEMENT}
                    rows={2}
                />

                <GsToggleField
                    label="Assignee → reviewer email"
                    icon={<FiMail size={14} />}
                    groupId={group.id}
                    fieldKey="allowAssigneeEmailNotification"
                    value={groupDetail?.aaen ?? false}
                    onUpdated={onUpdated}
                    disabled={emailDisabled}
                    disabledHint="Email notifications require a paid plan"
                    note="When enabled, assignees receive an email each time a reviewer approves or rejects their work."
                />

                <GsToggleField
                    label="Repeat download guard"
                    icon={<FiDownloadCloud size={14} />}
                    groupId={group.id}
                    fieldKey="dailyDownloadCapEnabled"
                    value={groupDetail?.ddce ?? true}
                    onUpdated={onUpdated}
                    note="When enabled, each member can only download the same file once per day. Repeated downloads within 24 hours are served from cache at no cost to the download budget."
                />

                <GsToggleField
                    label="Downgrade shield"
                    icon={<FiShield size={14} />}
                    groupId={group.id}
                    fieldKey="downgradeShielded"
                    value={groupDetail?.ds ?? false}
                    onUpdated={onUpdated}
                    note="Shielded groups are protected during plan downgrades — their files will be deleted last when storage must be reduced."
                />

                <GsImageSection group={group} ownerPlan={groupDetail?.op} onUpdated={onUpdated} />

                <GsDeleteSection group={group} onDeleted={onDeleted} />

                <div className="popup-actions" style={{ marginTop: 16 }}>
                    <button className="btn-secondary" onClick={onClose}>Close</button>
                </div>
            </div>
        </div>
    );
}
