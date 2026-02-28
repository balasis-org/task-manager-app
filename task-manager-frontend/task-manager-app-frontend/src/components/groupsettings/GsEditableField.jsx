// Generic field editor — pass selectOptions for a dropdown, otherwise textarea.
// transformValue lets the parent massage the value before the PATCH.
import { useState } from "react";
import { FiEdit2, FiCheck, FiX } from "react-icons/fi";
import { apiPatch } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import "@styles/groupsettings/GsEditableField.css";

export default function GsEditableField({
    label,
    groupId,
    fieldKey,
    initialValue,
    emptyText,
    onUpdated,
    maxLength,
    rows = 3,
    selectOptions,
    transformValue,
}) {
    const showToast = useToast();
    const [editing, setEditing] = useState(false);
    const [value, setValue] = useState(initialValue);
    const [saving, setSaving] = useState(false);

    const isSelect = !!selectOptions;

    async function save() {
        if (value == null) return; // shouldn't happen, but guard against stale renders
        setSaving(true);
        try {
            const raw = isSelect ? value : (typeof value === "string" ? value.trim() : value);
            const payload = { [fieldKey]: transformValue ? transformValue(raw) : raw };
            const updated = await apiPatch(`/api/groups/${groupId}`, payload);
            onUpdated(updated);
            showToast(`${label} updated`, "success");
            setEditing(false);
        } catch (err) {
            showToast(err?.message || `Failed to update ${label.toLowerCase()}`);
        } finally {
            setSaving(false);
        }
    }

    function cancel() {
        setEditing(false);
        setValue(initialValue);
    }

    function displayValue() {
        if (isSelect) {
            const opt = selectOptions.find((o) => o.value === value);
            return opt?.label || String(value);
        }
        return value || <em className="muted">{emptyText || "Not set"}</em>;
    }

    return (
        <section className="gs-section">
            <div className="gs-section-header">
                <span className="gs-section-label">{label}</span>
                {!editing && (
                    <button className="gs-edit-btn" onClick={() => setEditing(true)} title="Edit">
                        <FiEdit2 size={13} />
                    </button>
                )}
            </div>
            {editing ? (
                <div className="gs-field-edit">
                    {isSelect ? (
                        <select value={value} onChange={(e) => setValue(e.target.value)}>
                            {selectOptions.map((o) => (
                                <option key={o.value} value={o.value}>{o.label}</option>
                            ))}
                        </select>
                    ) : (
                        <>
                            <textarea
                                value={value}
                                onChange={(e) => setValue(e.target.value)}
                                rows={rows}
                                maxLength={maxLength}
                            />
                            {maxLength && (
                                <span className="char-count">{value.length}/{maxLength}</span>
                            )}
                        </>
                    )}
                    <div className="gs-field-actions">
                        <button className="gs-save-btn" onClick={save} disabled={saving}>
                            <FiCheck size={13} /> {saving ? "Saving…" : "Save"}
                        </button>
                        <button className="gs-cancel-btn" onClick={cancel}>
                            <FiX size={13} /> Cancel
                        </button>
                    </div>
                </div>
            ) : (
                <p className="gs-field-value">{displayValue()}</p>
            )}
        </section>
    );
}
