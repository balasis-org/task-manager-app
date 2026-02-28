import { LIMITS } from "@assets/js/inputValidation";
import GsEditableField from "@components/groupsettings/GsEditableField";
import GsImageSection from "@components/groupsettings/GsImageSection";
import GsTransferSection from "@components/groupsettings/GsTransferSection";
import GsDeleteSection from "@components/groupsettings/GsDeleteSection";
import "@styles/popups/Popup.css";
import "@styles/popups/GroupSettingsPopup.css";

export default function GroupSettingsPopup({ group, members, user, onClose, onUpdated, onDeleted }) {
    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card popup-card-wide gs-popup" onClick={e => e.stopPropagation()}>
                <h2 title={group.name} className="popup-heading-ellipsis">
                    {group.name} — Settings
                </h2>

                <GsEditableField
                    label="Description"
                    groupId={group.id}
                    fieldKey="description"
                    initialValue={group.description || ""}
                    emptyText="No description"
                    onUpdated={onUpdated}
                    maxLength={LIMITS.GROUP_DESCRIPTION}
                    rows={3}
                />

                <GsEditableField
                    label="Announcement"
                    groupId={group.id}
                    fieldKey="announcement"
                    initialValue={group.announcement || ""}
                    emptyText="No announcement"
                    onUpdated={onUpdated}
                    maxLength={LIMITS.GROUP_ANNOUNCEMENT}
                    rows={2}
                />

                <GsEditableField
                    label="Email notifications"
                    groupId={group.id}
                    fieldKey="allowEmailNotification"
                    initialValue={(group.allowEmailNotification ?? true) ? "on" : "off"}
                    onUpdated={onUpdated}
                    selectOptions={[{ value: "on", label: "On" }, { value: "off", label: "Off" }]}
                    transformValue={v => v === "on"}
                />

                <GsImageSection group={group} onUpdated={onUpdated} />

                <GsTransferSection
                    group={group}
                    members={members}
                    user={user}
                    onClose={onClose}
                />

                <GsDeleteSection group={group} onDeleted={onDeleted} />

                <div className="popup-actions" style={{ marginTop: 16 }}>
                    <button className="btn-secondary" onClick={onClose}>Close</button>
                </div>
            </div>
        </div>
    );
}
