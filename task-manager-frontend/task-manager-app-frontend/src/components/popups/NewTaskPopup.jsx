import { useState, useRef, useEffect } from "react";
import { apiMultipart } from "@assets/js/apiClient";
import { LIMITS } from "@assets/js/inputValidation";
import { isFileTooLarge, getFileIcon } from "@assets/js/fileUtils";
import blobBase from "@blobBase";
import { FiX, FiPlus, FiSearch } from "react-icons/fi";
import "@styles/popups/Popup.css";

const STATE_OPTIONS = [
    { value: "TODO", label: "TODO" },
    { value: "IN_PROGRESS", label: "In Progress" },
    { value: "TO_BE_REVIEWED", label: "To Be Reviewed" },
    { value: "DONE", label: "Done" },
];

const REVIEWER_ELIGIBLE_ROLES = ["REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];
const ASSIGNEE_ELIGIBLE_ROLES = ["MEMBER", "REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];

function userImg(u) {
    if (!u) return "";
    return u.imgUrl ? blobBase + u.imgUrl : u.defaultImgUrl ? blobBase + u.defaultImgUrl : "";
}

export default function NewTaskPopup({ groupId, initialState, members, onClose, onCreated, onRefresh }) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [taskState, setTaskState] = useState(initialState || "TODO");
    const [priority, setPriority] = useState("");
    const [dueDate, setDueDate] = useState("");
    const [selectedReviewers, setSelectedReviewers] = useState([]);
    const [selectedAssignees, setSelectedAssignees] = useState([]);
    const [files, setFiles] = useState([]);
    const [reviewerSearch, setReviewerSearch] = useState("");
    const [assigneeSearch, setAssigneeSearch] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [fileDragOver, setFileDragOver] = useState(false);
    const fileInputRef = useRef(null);

    // filter out already-picked members
    const reviewerIds = new Set(selectedReviewers.map((m) => m.user?.id));
    const assigneeIds = new Set(selectedAssignees.map((m) => m.user?.id));
    const eligibleReviewers = (members || []).filter(
        (m) => REVIEWER_ELIGIBLE_ROLES.includes(m.role) && !reviewerIds.has(m.user?.id)
              && (!reviewerSearch || (m.user?.name || "").toLowerCase().includes(reviewerSearch.toLowerCase())
                                  || (m.user?.email || "").toLowerCase().includes(reviewerSearch.toLowerCase()))
    );
    const eligibleAssignees = (members || []).filter(
        (m) => ASSIGNEE_ELIGIBLE_ROLES.includes(m.role) && !assigneeIds.has(m.user?.id)
              && (!assigneeSearch || (m.user?.name || "").toLowerCase().includes(assigneeSearch.toLowerCase())
                                  || (m.user?.email || "").toLowerCase().includes(assigneeSearch.toLowerCase()))
    );

    // if someone leaves the group mid-edit, remove them from selected
    useEffect(() => {
        const currentMemberIds = new Set((members || []).map((m) => m.user?.id));
        setSelectedReviewers((prev) => prev.filter((m) => currentMemberIds.has(m.user?.id)));
        setSelectedAssignees((prev) => prev.filter((m) => currentMemberIds.has(m.user?.id)));
    }, [members]);

    function addReviewer(m) { setSelectedReviewers((prev) => [...prev, m]); }
    function removeReviewer(id) { setSelectedReviewers((prev) => prev.filter((m) => m.user?.id !== id)); }
    function addAssignee(m) { setSelectedAssignees((prev) => [...prev, m]); }
    function removeAssignee(id) { setSelectedAssignees((prev) => prev.filter((m) => m.user?.id !== id)); }

    function handleFileAdd(e) {
        const picked = Array.from(e.target.files || []);
        addFiles(picked);
        e.target.value = "";
    }

    function addFiles(incoming) {
        const oversized = incoming.filter(isFileTooLarge);
        if (oversized.length) {
            setError(`File(s) exceed ${LIMITS.MAX_FILE_SIZE_MB} MB limit: ${oversized.map(f => f.name).join(", ")}`);
            return;
        }
        setFiles((prev) => {
            const combined = [...prev, ...incoming];
            if (combined.length > LIMITS.MAX_TASK_FILES) {
                setError(`Maximum ${LIMITS.MAX_TASK_FILES} files allowed.`);
            }
            return combined.slice(0, LIMITS.MAX_TASK_FILES);
        });
    }

    function handleFileDrop(e) {
        e.preventDefault();
        setFileDragOver(false);
        const dropped = Array.from(e.dataTransfer.files || []);
        if (!dropped.length) return;
        addFiles(dropped);
    }

    function removeFile(idx) { setFiles((prev) => prev.filter((_, i) => i !== idx)); }

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!title.trim()) { setError("Title is required."); return; }
        if (!description.trim()) { setError("Description is required."); return; }

        setBusy(true);
        setError("");
        try {
            const payload = {
                title: title.trim(),
                description: description.trim(),
                taskState,
            };
            if (priority !== "") payload.priority = Number(priority);
            if (dueDate) payload.dueDate = dueDate + "T00:00:00";
            if (selectedAssignees.length) payload.assignedIds = selectedAssignees.map((m) => m.user?.id);
            if (selectedReviewers.length) payload.reviewerIds = selectedReviewers.map((m) => m.user?.id);

            const fd = new FormData();
            fd.append(
                "data",
                new Blob([JSON.stringify(payload)], { type: "application/json" })
            );
            for (const f of files) fd.append("files", f);

            const created = await apiMultipart(`/api/groups/${groupId}/tasks`, fd);
            onCreated(created);
        } catch (err) {
            setError(err?.message || "Failed to create task.");
            // refresh members so removed people get cleared
            if (onRefresh) onRefresh();
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="popup-overlay" onClick={onClose}>
            <div className="popup-card popup-card-wide" onClick={(e) => e.stopPropagation()}>
                <h2>New task</h2>

                {error && <div className="popup-error">{error}</div>}

                <form onSubmit={handleSubmit} className="popup-form">
                    <label>
                        Title
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            maxLength={150}
                            autoFocus
                            required
                        />
                    </label>

                    <label>
                        Description
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            maxLength={LIMITS.TASK_DESCRIPTION}
                            required
                        />
                        <span className="char-count">{description.length}/{LIMITS.TASK_DESCRIPTION}</span>
                    </label>

                    <div className="popup-form-row">
                        <label className="popup-form-half">
                            State
                            <select value={taskState} onChange={(e) => setTaskState(e.target.value)}>
                                {STATE_OPTIONS.map((s) => (
                                    <option key={s.value} value={s.value}>{s.label}</option>
                                ))}
                            </select>
                        </label>

                        <label className="popup-form-half">
                            Priority (0–10)
                            <input
                                type="number"
                                min={0}
                                max={10}
                                value={priority}
                                onChange={(e) => setPriority(e.target.value)}
                                placeholder="—"
                            />
                        </label>
                    </div>

                    <label>
                        Due date
                        <input
                            type="date"
                            value={dueDate}
                            onChange={(e) => setDueDate(e.target.value)}
                        />
                    </label>

                    {/* reviewers */}
                    <div className="popup-picker-section">
                        <span className="popup-picker-label">
                            Reviewers
                            <span className="popup-picker-search">
                                <FiSearch size={11} />
                                <input
                                    type="text"
                                    placeholder="Search…"
                                    value={reviewerSearch}
                                    onChange={(e) => setReviewerSearch(e.target.value)}
                                />
                            </span>
                        </span>
                        {selectedReviewers.length > 0 && (
                            <div className="popup-chip-list">
                                {selectedReviewers.map((m) => (
                                    <span key={m.user?.id} className="popup-chip" title={m.user?.email}>
                                        <img src={userImg(m.user)} alt="" className="popup-chip-img" />
                                        {m.user?.name || m.user?.email}
                                        <button type="button" className="popup-chip-rm" onClick={() => removeReviewer(m.user?.id)}><FiX size={10} /></button>
                                    </span>
                                ))}
                            </div>
                        )}
                        {eligibleReviewers.length > 0 && (
                            <div className="popup-picker-dropdown">
                                {eligibleReviewers.map((m) => (
                                    <div key={m.user?.id} className="popup-picker-item" onClick={() => addReviewer(m)}>
                                        <img src={userImg(m.user)} alt="" className="popup-search-img" />
                                        <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                        <span className="popup-picker-role">{m.role.replace(/_/g, " ")}</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* assignees */}
                    <div className="popup-picker-section">
                        <span className="popup-picker-label">
                            Assignees
                            <span className="popup-picker-search">
                                <FiSearch size={11} />
                                <input
                                    type="text"
                                    placeholder="Search…"
                                    value={assigneeSearch}
                                    onChange={(e) => setAssigneeSearch(e.target.value)}
                                />
                            </span>
                        </span>
                        {selectedAssignees.length > 0 && (
                            <div className="popup-chip-list">
                                {selectedAssignees.map((m) => (
                                    <span key={m.user?.id} className="popup-chip" title={m.user?.email}>
                                        <img src={userImg(m.user)} alt="" className="popup-chip-img" />
                                        {m.user?.name || m.user?.email}
                                        <button type="button" className="popup-chip-rm" onClick={() => removeAssignee(m.user?.id)}><FiX size={10} /></button>
                                    </span>
                                ))}
                            </div>
                        )}
                        {eligibleAssignees.length > 0 && (
                            <div className="popup-picker-dropdown">
                                {eligibleAssignees.map((m) => (
                                    <div key={m.user?.id} className="popup-picker-item" onClick={() => addAssignee(m)}>
                                        <img src={userImg(m.user)} alt="" className="popup-search-img" />
                                        <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                                        <span className="popup-picker-role">{m.role.replace(/_/g, " ")}</span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* file attachments */}
                    <div
                        className={`popup-picker-section${fileDragOver ? " file-drag-over" : ""}`}
                        onDragOver={(e) => { e.preventDefault(); setFileDragOver(true); }}
                        onDragLeave={() => setFileDragOver(false)}
                        onDrop={handleFileDrop}
                    >
                        <span className="popup-picker-label">
                            Files ({files.length}/{LIMITS.MAX_TASK_FILES})
                            {files.length < LIMITS.MAX_TASK_FILES && (
                                <button type="button" className="popup-chip-add" onClick={() => fileInputRef.current?.click()} title="Add file">
                                    <FiPlus size={12} />
                                </button>
                            )}
                        </span>
                        <input ref={fileInputRef} type="file" hidden onChange={handleFileAdd} />
                        {files.length > 0 && (
                            <div className="popup-chip-list">
                                {files.map((f, i) => {
                                    const Icon = getFileIcon(f.name);
                                    return (
                                        <span key={i} className="popup-chip" title={f.name}>
                                            <Icon size={12} />
                                            {f.name}
                                            <button type="button" className="popup-chip-rm" onClick={() => removeFile(i)}><FiX size={10} /></button>
                                        </span>
                                    );
                                })}
                            </div>
                        )}
                        {files.length < LIMITS.MAX_TASK_FILES && (
                            <span className="popup-drop-hint">or drag & drop files here</span>
                        )}
                    </div>

                    <div className="popup-actions">
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={onClose}
                            disabled={busy}
                        >
                            Cancel
                        </button>
                        <button type="submit" className="btn-primary" disabled={busy}>
                            {busy ? "Creating…" : "Create task"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
