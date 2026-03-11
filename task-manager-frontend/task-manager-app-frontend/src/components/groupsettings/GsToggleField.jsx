import { useState, useRef, useEffect } from "react";
import { apiPatch } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import "@styles/groupsettings/GsToggleField.css";

export default function GsToggleField({ label, groupId, fieldKey, value, onUpdated, disabled, disabledHint, note }) {
    const showToast = useToast();
    const [on, setOn] = useState(value);
    const timerRef = useRef(null);

    useEffect(() => { setOn(value); }, [value]);

    function toggle() {
        if (disabled) return;
        const next = !on;
        setOn(next);
        clearTimeout(timerRef.current);
        timerRef.current = setTimeout(async () => {
            try {
                const updated = await apiPatch(`/api/groups/${groupId}`, { [fieldKey]: next });
                onUpdated(updated);
            } catch (err) {
                setOn(!next);
                showToast(err?.message || `Failed to update ${label.toLowerCase()}`);
            }
        }, 1000);
    }

    useEffect(() => () => clearTimeout(timerRef.current), []);

    return (
        <section className="gs-section">
            <div className="gs-section-header">
                <span className="gs-section-label">{label}</span>
                <button
                    type="button"
                    role="switch"
                    aria-checked={on && !disabled}
                    className={`gs-toggle${on && !disabled ? " gs-toggle-on" : ""}${disabled ? " gs-toggle-disabled" : ""}`}
                    onClick={toggle}
                    disabled={disabled}
                    title={disabled ? disabledHint : `${on ? "Disable" : "Enable"} ${label.toLowerCase()}`}
                >
                    <span className="gs-toggle-knob" />
                </button>
            </div>
            {disabled && disabledHint && (
                <p className="gs-toggle-hint">{disabledHint}</p>
            )}
            {note && <p className="gs-toggle-note">{note}</p>}
        </section>
    );
}
