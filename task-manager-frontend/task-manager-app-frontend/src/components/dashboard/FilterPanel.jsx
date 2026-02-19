import { useState, useRef, useEffect, useCallback } from "react";
import { FiX, FiInfo, FiEdit2, FiCheck } from "react-icons/fi";
import "@styles/dashboard/FilterPanel.css";

const PRIORITY_MIN = 1;
const PRIORITY_MAX = 10;

const STATE_OPTIONS = [
    { value: "", label: "Any" },
    { value: "TODO", label: "TODO" },
    { value: "IN_PROGRESS", label: "In progress" },
    { value: "TO_BE_REVIEWED", label: "To be reviewed" },
    { value: "DONE", label: "Done" },
];

const HAS_FILES_OPTIONS = [
    { value: "", label: "Any" },
    { value: "true", label: "Yes" },
    { value: "false", label: "No" },
];

const EMPTY = {
    creatorId: "",
    reviewerId: "",
    assigneeId: "",
    priorityMin: "",
    priorityMax: "",
    taskState: "",
    hasFiles: "",
    dueDateBefore: "",
};

function isFilterEmpty(f) {
    return Object.values(f).every((v) => v === "" || v == null);
}

/**
 * FilterPanel — user sets criteria then clicks Apply once.
 * While applied, fields are locked. "Edit" unlocks them.
 *
 * Props:
 *  - members       : cached members list for pickers
 *  - filters        : current filter criteria object
 *  - isApplied      : boolean — are filters currently applied / locked?
 *  - onDraftChange  : (draft) => void — updates draft criteria (no request)
 *  - onApply        : () => void — fire the backend request
 *  - onEdit         : () => void — unlock fields (marks not-applied)
 *  - onClear        : () => void — reset everything
 *  - onClose        : () => void — close the panel
 */
export default function FilterPanel({
    members,
    filters,
    isApplied,
    onDraftChange,
    onApply,
    onEdit,
    onClear,
    onClose,
}) {
    const panelRef = useRef(null);
    const locked = isApplied;

    // member search states for each picker
    const [creatorSearch, setCreatorSearch] = useState("");
    const [reviewerSearch, setReviewerSearch] = useState("");
    const [assigneeSearch, setAssigneeSearch] = useState("");

    // click-outside closes the panel
    useEffect(() => {
        function handleClick(e) {
            if (panelRef.current && !panelRef.current.contains(e.target)) {
                onClose();
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, [onClose]);

    const set = useCallback(
        (key, value) => onDraftChange({ ...filters, [key]: value }),
        [filters, onDraftChange]
    );

    const handleClear = () => {
        setCreatorSearch("");
        setReviewerSearch("");
        setAssigneeSearch("");
        onClear();
    };

    // member helpers
    const memberName = (id) => {
        if (!id) return "";
        const m = members.find((m) => String(m.user?.id) === String(id));
        return m?.user?.name || m?.user?.email || `User #${id}`;
    };

    const filteredMembers = (search) => {
        if (!search.trim()) return members;
        const q = search.toLowerCase();
        return members.filter(
            (m) =>
                (m.user?.name || "").toLowerCase().includes(q) ||
                (m.user?.email || "").toLowerCase().includes(q)
        );
    };

    const empty = isFilterEmpty(filters);

    return (
        <div className="filter-panel" ref={panelRef}>
            <div className="filter-panel-header">
                <span className="filter-panel-title">Filters</span>
                {isApplied && (
                    <span className="filter-active-badge">Active</span>
                )}
                <button className="filter-panel-close" onClick={onClose} title="Close">
                    <FiX size={14} />
                </button>
            </div>

            <div className="filter-panel-info">
                <FiInfo size={12} />
                <span>
                    {locked
                        ? "Filters are applied. Press Edit to change them."
                        : "Set your criteria and press Apply."}
                </span>
            </div>

            <div className={`filter-panel-body${locked ? " locked" : ""}`}>
                {/* Creator picker */}
                <MemberPicker
                    label="Creator"
                    value={filters.creatorId}
                    onSelect={(id) => set("creatorId", id)}
                    members={members}
                    search={creatorSearch}
                    onSearchChange={setCreatorSearch}
                    memberName={memberName}
                    filteredMembers={filteredMembers}
                    disabled={locked}
                />

                {/* Reviewer picker */}
                <MemberPicker
                    label="Reviewer"
                    value={filters.reviewerId}
                    onSelect={(id) => set("reviewerId", id)}
                    members={members}
                    search={reviewerSearch}
                    onSearchChange={setReviewerSearch}
                    memberName={memberName}
                    filteredMembers={filteredMembers}
                    disabled={locked}
                />

                {/* Assignee picker */}
                <MemberPicker
                    label="Assignee"
                    value={filters.assigneeId}
                    onSelect={(id) => set("assigneeId", id)}
                    members={members}
                    search={assigneeSearch}
                    onSearchChange={setAssigneeSearch}
                    memberName={memberName}
                    filteredMembers={filteredMembers}
                    disabled={locked}
                />

                {/* Priority range */}
                <div className="filter-field filter-priority-range">
                    <span className="filter-field-label">Priority (1–10)</span>
                    <div className="filter-range-inputs">
                        <input
                            type="number"
                            min={PRIORITY_MIN}
                            max={PRIORITY_MAX}
                            placeholder="Min"
                            value={filters.priorityMin}
                            onChange={(e) => set("priorityMin", e.target.value)}
                            disabled={locked}
                            className="filter-range-input"
                        />
                        <span className="filter-range-sep">–</span>
                        <input
                            type="number"
                            min={PRIORITY_MIN}
                            max={PRIORITY_MAX}
                            placeholder="Max"
                            value={filters.priorityMax}
                            onChange={(e) => set("priorityMax", e.target.value)}
                            disabled={locked}
                            className="filter-range-input"
                        />
                    </div>
                </div>

                {/* Task State */}
                <label className="filter-field">
                    <span className="filter-field-label">Status</span>
                    <select
                        value={filters.taskState}
                        onChange={(e) => set("taskState", e.target.value)}
                        disabled={locked}
                    >
                        {STATE_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.label}
                            </option>
                        ))}
                    </select>
                </label>

                {/* Has Files */}
                <label className="filter-field">
                    <span className="filter-field-label">Has files</span>
                    <select
                        value={filters.hasFiles}
                        onChange={(e) => set("hasFiles", e.target.value)}
                        disabled={locked}
                    >
                        {HAS_FILES_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.label}
                            </option>
                        ))}
                    </select>
                </label>

                {/* Due Date Before */}
                <label className="filter-field">
                    <span className="filter-field-label">Due before</span>
                    <input
                        type="date"
                        value={filters.dueDateBefore}
                        onChange={(e) => set("dueDateBefore", e.target.value)}
                        disabled={locked}
                    />
                </label>
            </div>

            <div className="filter-panel-footer">
                {locked ? (
                    /* Active state — Edit or Clear */
                    <>
                        <button className="filter-edit-btn" onClick={onEdit}>
                            <FiEdit2 size={11} /> Edit
                        </button>
                        <button className="filter-clear-btn" onClick={handleClear}>
                            Clear all
                        </button>
                    </>
                ) : (
                    /* Draft state — Apply or Clear */
                    <>
                        <button
                            className="filter-apply-btn"
                            onClick={onApply}
                            disabled={empty}
                        >
                            <FiCheck size={12} /> Apply
                        </button>
                        <button
                            className="filter-clear-btn"
                            onClick={handleClear}
                            disabled={empty}
                        >
                            Clear all
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}


/* --- Member picker sub-component --- */

function MemberPicker({
    label,
    value,
    onSelect,
    members,
    search,
    onSearchChange,
    memberName,
    filteredMembers,
    disabled,
}) {
    const [open, setOpen] = useState(false);
    const wrapperRef = useRef(null);

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

export { EMPTY as FILTER_EMPTY, isFilterEmpty };
