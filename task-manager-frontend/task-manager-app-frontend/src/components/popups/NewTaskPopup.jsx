import { useState, useEffect } from "react";
import { apiMultipart } from "@assets/js/apiClient";
import { LIMITS } from "@assets/js/inputValidation";
import { useBlobUrl } from "@context/BlobSasContext";
import NtMemberPicker from "@components/newtask/NtMemberPicker";
import NtFileSection from "@components/newtask/NtFileSection";
import "@styles/popups/Popup.css";
import "@styles/popups/NewTaskPopup.css";

const STATE_OPTIONS = [
    { value: "TODO", label: "TODO" },
    { value: "IN_PROGRESS", label: "In Progress" },
    { value: "TO_BE_REVIEWED", label: "To Be Reviewed" },
    { value: "DONE", label: "Done" },
];

const REVIEWER_ELIGIBLE_ROLES = ["REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];
const ASSIGNEE_ELIGIBLE_ROLES = ["MEMBER", "REVIEWER", "TASK_MANAGER", "GROUP_LEADER"];

export default function NewTaskPopup({ groupId, initialState, members, groupDetail, onClose, onCreated, onRefresh, maxCreatorFiles, maxFileSizeBytes }) {
    const blobUrl = useBlobUrl();
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

    useEffect(() => {
        const currentMemberIds = new Set((members || []).map((m) => m.user?.id));
        setSelectedReviewers((prev) => prev.filter((m) => currentMemberIds.has(m.user?.id)));
        setSelectedAssignees((prev) => prev.filter((m) => currentMemberIds.has(m.user?.id)));
    }, [members]);

    function addReviewer(m) { setSelectedReviewers((prev) => [...prev, m]); }
    function removeReviewer(id) { setSelectedReviewers((prev) => prev.filter((m) => m.user?.id !== id)); }
    function addAssignee(m) { setSelectedAssignees((prev) => [...prev, m]); }
    function removeAssignee(id) { setSelectedAssignees((prev) => prev.filter((m) => m.user?.id !== id)); }

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
            if (dueDate) payload.dueDate = new Date(dueDate).toISOString();
            if (selectedAssignees.length) payload.assignedIds = selectedAssignees.map((m) => m.user?.id);
            if (selectedReviewers.length) payload.reviewerIds = selectedReviewers.map((m) => m.user?.id);

            const fd = new FormData();
            fd.append(
                "data",
                new Blob([JSON.stringify(payload)], { type: "application/json; charset=UTF-8"})
            );
            for (const f of files) fd.append("files", f);

            const created = await apiMultipart(`/api/groups/${groupId}/tasks`, fd);
            onCreated(created);
        } catch (err) {
            setError(err?.message || "Failed to create task.");

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

                {groupDetail?.op && groupDetail.op !== "FREE" && groupDetail.op !== "STUDENT" && (
                    <div className="nt-email-info">
                        <span>Email task notifications: <strong>{groupDetail.aen ? "On" : "Off"}</strong></span>
                        <span>Assignee → reviewer: <strong>{groupDetail.aaen ? "On" : "Off"}</strong></span>
                        <p className="nt-email-info-hint">The group leader may change these in group settings.</p>
                    </div>
                )}

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
                            onChange={(e) => {setDescription(e.target.value)}}
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
                            Priority (0-10)
                            <input
                                type="number"
                                min={0}
                                max={10}
                                value={priority}
                                onChange={(e) => setPriority(e.target.value)}
                                placeholder="-"
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

                    { }
                    <NtMemberPicker
                        label="Reviewers"
                        selected={selectedReviewers}
                        eligible={eligibleReviewers}
                        search={reviewerSearch}
                        onSearchChange={setReviewerSearch}
                        onAdd={addReviewer}
                        onRemove={removeReviewer}
                        blobUrl={blobUrl}
                    />

                    { }
                    <NtMemberPicker
                        label="Assignees"
                        selected={selectedAssignees}
                        eligible={eligibleAssignees}
                        search={assigneeSearch}
                        onSearchChange={setAssigneeSearch}
                        onAdd={addAssignee}
                        onRemove={removeAssignee}
                        blobUrl={blobUrl}
                    />

                    { }
                    <NtFileSection
                        files={files}
                        onFilesChange={setFiles}
                        onError={setError}
                        maxFiles={maxCreatorFiles}
                        maxFileSizeBytes={maxFileSizeBytes}
                    />

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
