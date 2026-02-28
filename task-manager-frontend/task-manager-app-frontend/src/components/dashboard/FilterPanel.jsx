import { useState, useRef, useEffect, useCallback } from "react";
import { FiX, FiInfo, FiEdit2, FiCheck } from "react-icons/fi";
import FilterMemberPicker from "@components/filterpanel/FilterMemberPicker";
import "@styles/dashboard/FilterPanel.css";

const PRIORITY_MIN = 1;
const PRIORITY_MAX = 10; // TODO: pull from backend config if it ever becomes dynamic

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

    const [creatorSearch, setCreatorSearch] = useState("");
    const [reviewerSearch, setReviewerSearch] = useState("");
    const [assigneeSearch, setAssigneeSearch] = useState("");

    // mousedown so the panel closes before click bubbles to other elements
    useEffect(() => {
        function handleClick(e) {
            if (panelRef.current && !panelRef.current.contains(e.target)) {
                onClose();
            }
        }
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, [onClose]);

    const updateFilter = useCallback(
        (key, value) => onDraftChange({ ...filters, [key]: value }),
        [filters, onDraftChange]
    );

    const handleClear = () => {
        setCreatorSearch("");
        setReviewerSearch("");
        setAssigneeSearch("");
        onClear();
    };

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
                { }
                <FilterMemberPicker
                    label="Creator"
                    value={filters.creatorId}
                    onSelect={(id) => updateFilter("creatorId", id)}
                    search={creatorSearch}
                    onSearchChange={setCreatorSearch}
                    memberName={memberName}
                    filteredMembers={filteredMembers}
                    disabled={locked}
                />

                { }
                <FilterMemberPicker
                    label="Reviewer"
                    value={filters.reviewerId}
                    onSelect={(id) => updateFilter("reviewerId", id)}
                    search={reviewerSearch}
                    onSearchChange={setReviewerSearch}
                    memberName={memberName}
                    filteredMembers={filteredMembers}
                    disabled={locked}
                />

                { }
                <FilterMemberPicker
                    label="Assignee"
                    value={filters.assigneeId}
                    onSelect={(id) => updateFilter("assigneeId", id)}
                    search={assigneeSearch}
                    onSearchChange={setAssigneeSearch}
                    memberName={memberName}
                    filteredMembers={filteredMembers}
                    disabled={locked}
                />

                { }
                <div className="filter-field filter-priority-range">
                    <span className="filter-field-label">Priority (1–10)</span>
                    <div className="filter-range-inputs">
                        <input
                            type="number"
                            min={PRIORITY_MIN}
                            max={PRIORITY_MAX}
                            placeholder="Min"
                            value={filters.priorityMin}
                            onChange={(e) => updateFilter("priorityMin", e.target.value)}
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
                            onChange={(e) => updateFilter("priorityMax", e.target.value)}
                            disabled={locked}
                            className="filter-range-input"
                        />
                    </div>
                </div>

                { }
                <label className="filter-field">
                    <span className="filter-field-label">Status</span>
                    <select
                        value={filters.taskState}
                        onChange={(e) => updateFilter("taskState", e.target.value)}
                        disabled={locked}
                    >
                        {STATE_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.label}
                            </option>
                        ))}
                    </select>
                </label>

                { }
                <label className="filter-field">
                    <span className="filter-field-label">Has files</span>
                    <select
                        value={filters.hasFiles}
                        onChange={(e) => updateFilter("hasFiles", e.target.value)}
                        disabled={locked}
                    >
                        {HAS_FILES_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.label}
                            </option>
                        ))}
                    </select>
                </label>

                { }
                <label className="filter-field">
                    <span className="filter-field-label">Due before</span>
                    <input
                        type="date"
                        value={filters.dueDateBefore}
                        onChange={(e) => updateFilter("dueDateBefore", e.target.value)}
                        disabled={locked}
                    />
                </label>
            </div>

            <div className="filter-panel-footer">
                {locked ? (

                    <>
                        <button className="filter-edit-btn" onClick={onEdit}>
                            <FiEdit2 size={11} /> Edit
                        </button>
                        <button className="filter-clear-btn" onClick={handleClear}>
                            Clear all
                        </button>
                    </>
                ) : (

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

// Dashboard.jsx imports these to check whether filters are active before fetching
export { EMPTY as FILTER_EMPTY, isFilterEmpty };
