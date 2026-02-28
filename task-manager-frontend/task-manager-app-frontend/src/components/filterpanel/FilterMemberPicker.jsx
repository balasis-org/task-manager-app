// Dropdown for picking a member by name/email — used in the filter panel
import { useState, useRef, useEffect } from "react";
import { FiX } from "react-icons/fi";
import "@styles/filterpanel/FilterMemberPicker.css";

export default function FilterMemberPicker({
    label,
    value,
    onSelect,
    search,
    onSearchChange,
    memberName,
    filteredMembers,
    disabled,
}) {
    const [open, setOpen] = useState(false);
    const wrapperRef = useRef(null);

    // close on outside click (same pattern as TopBarGroupSelector)
    useEffect(() => {
        function handleClick(e) {
            if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
                setOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, []);

    const handleSelect = (id) => {
        onSelect(id);
        setOpen(false);
        onSearchChange("");
    };

    return (
        <div className="filter-field filter-member-picker" ref={wrapperRef}>
            <span className="filter-field-label">{label}</span>
            <button
                className="filter-member-btn"
                type="button"
                onClick={() => { if (!disabled) setOpen((v) => !v); }}
                disabled={disabled}
            >
                {value ? memberName(value) : "Any"}
                <span className="caret">▾</span>
            </button>
            {value && !disabled && (
                <button
                    className="filter-member-clear"
                    onClick={(e) => {
                        e.stopPropagation();
                        onSelect("");
                    }}
                    title="Clear"
                >
                    <FiX size={10} />
                </button>
            )}

            {open && !disabled && (
                <div className="filter-member-dropdown">
                    <input
                        type="text"
                        className="filter-member-search"
                        placeholder="Search…"
                        value={search}
                        onChange={(e) => onSearchChange(e.target.value)}
                        autoFocus
                    />
                    <div className="filter-member-list">
                        <div
                            className={`filter-member-item${!value ? " active" : ""}`}
                            onClick={() => handleSelect("")}
                        >
                            Any
                        </div>
                        {filteredMembers(search).map((m) => (
                            <div
                                key={m.user?.id}
                                className={`filter-member-item${
                                    String(m.user?.id) === String(value) ? " active" : ""
                                }`}
                                onClick={() => handleSelect(String(m.user?.id))}
                            >
                                {m.user?.name || m.user?.email}
                                <span className="filter-member-role">{m.role}</span>
                            </div>
                        ))}
                        {filteredMembers(search).length === 0 && (
                            <div className="filter-member-item muted">No members found</div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
