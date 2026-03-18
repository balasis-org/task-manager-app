// Shared between creator files and assignee files - the parent
// just passes different labels, limits, and handlers.
import { useRef, useState } from "react";
import { FiPlus, FiDownload, FiTrash2, FiCheck, FiAlertTriangle } from "react-icons/fi";
import { getFileIcon, formatFileSize } from "@assets/js/fileUtils";
import "@styles/task/TaskFilesSection.css";

// drag-and-drop + click-to-upload file section with inline file review UI.
// reused for both creator files and assignee files — the parent passes
// different labels/limits/handlers. REVIEW_NOTE_MAX is a local UI cap,
// not enforced by the backend.
const REVIEW_NOTE_MAX = 200;

export default function TaskFilesSection({
    files = [],
    maxFiles,
    canUpload,
    canReview,
    label,
    emptyText,
    className,
    onFileAdd,
    onDownload,
    onDelete,
    onFileReview,
    downloadingId,
}) {
    const inputRef = useRef(null);
    const [dragOver, setDragOver] = useState(false);
    const [reviewOpenId, setReviewOpenId] = useState(null);
    const [reviewNote, setReviewNote] = useState("");

    const count = files.length;
    const canAdd = canUpload && count < maxFiles;

    function submitReview(fileId, status) {
        if (!onFileReview) return;
        onFileReview(fileId, status, reviewNote.trim() || null);
        setReviewOpenId(null);
        setReviewNote("");
    }

    function handleDrop(e) {
        e.preventDefault();
        setDragOver(false);
        const file = e.dataTransfer.files?.[0];
        if (file) onFileAdd(file);
    }

    function handleInputChange(e) {
        const file = e.target.files[0];
        if (file) onFileAdd(file);
    }

    return (
        <div
            className={`${className}${dragOver ? " drag-over" : ""}`}
            onDragOver={(e) => { if (canUpload) { e.preventDefault(); setDragOver(true); } }}
            onDragLeave={() => setDragOver(false)}
            onDrop={(e) => { if (canUpload) handleDrop(e); }}
        >
            <h3>
                {label} ({count}/{maxFiles}):
                {canAdd && (
                    <button
                        className="task-file-add"
                        onClick={() => inputRef.current?.click()}
                        title={`Upload ${label.toLowerCase()}`}
                    >
                        <FiPlus size={12} />
                    </button>
                )}
            </h3>

            <input ref={inputRef} type="file" hidden onChange={handleInputChange} />

            {count > 0 ? (
                <ul className="task-file-list">
                    {/* spread into a new array - files is a prop, we don't want to
                   accidentally mutate the parent's list during drag-and-drop */}
                {[...files].map((f) => {
                        const Icon = getFileIcon(f.name);
                        const latestReview = f.reviews?.length ? f.reviews[f.reviews.length - 1] : null;
                        return (
                            <li key={f.id} className="task-file-item">
                                <button
                                    className="task-file-icon-btn"
                                    title="Download"
                                    onClick={() => onDownload(f.id, f.name)}
                                >
                                    <Icon size={16} />
                                    <span className="task-file-dl-overlay">
                                        <FiDownload size={9} />
                                    </span>
                                </button>

                                <span
                                    className="task-file-name"
                                    title={f.name}
                                    role="button"
                                    tabIndex={0}
                                    onClick={() => onDownload(f.id, f.name)}
                                    onKeyDown={(e) => {
                                        if (e.key === "Enter") onDownload(f.id, f.name);
                                    }}
                                >
                                    {f.name}
                                </span>

                                {latestReview && (
                                    <span
                                        className={`file-review-badge ${latestReview.status === "CHECKED" ? "badge-checked" : "badge-revision"}`}
                                        title={`${latestReview.status === "CHECKED" ? "Checked" : "Needs revision"} by ${latestReview.reviewerName || "reviewer"}${latestReview.note ? `: ${latestReview.note}` : ""}`}
                                    >
                                        {latestReview.status === "CHECKED"
                                            ? <><FiCheck size={10} /> OK</>
                                            : <><FiAlertTriangle size={10} /> Rev</>}
                                    </span>
                                )}

                                {f.fileSize != null && (
                                    <span className="task-file-size" title={`${f.fileSize} bytes`}>
                                        {formatFileSize(f.fileSize)}
                                    </span>
                                )}

                                {downloadingId === f.id && (
                                    <span className="task-file-downloading">↓</span>
                                )}

                                {canReview && onFileReview && (
                                    <button
                                        className="file-review-toggle"
                                        title="Review file"
                                        onClick={() => {
                                            setReviewOpenId(reviewOpenId === f.id ? null : f.id);
                                            setReviewNote("");
                                        }}
                                    >
                                        ✎
                                    </button>
                                )}

                                {canUpload && (
                                    <button
                                        className="task-file-rm"
                                        title="Remove"
                                        onClick={() => onDelete(f.id)}
                                    >
                                        <FiTrash2 size={13} />
                                    </button>
                                )}

                                {reviewOpenId === f.id && (
                                    <div className="file-review-inline">
                                        <input
                                            className="file-review-note"
                                            type="text"
                                            placeholder="Note (optional)"
                                            value={reviewNote}
                                            onChange={(e) => setReviewNote(e.target.value.slice(0, REVIEW_NOTE_MAX))}
                                            maxLength={REVIEW_NOTE_MAX}
                                        />
                                        <button className="file-review-btn btn-checked" onClick={() => submitReview(f.id, "CHECKED")} title="Mark as checked">
                                            <FiCheck size={12} /> OK
                                        </button>
                                        <button className="file-review-btn btn-revision" onClick={() => submitReview(f.id, "NEEDS_REVISION")} title="Needs revision">
                                            <FiAlertTriangle size={12} /> Rev
                                        </button>
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            ) : (
                <p className="task-no-files">{emptyText}</p>
            )}

            {canAdd && (
                <span className="task-drop-hint">Drop a file here to upload</span>
            )}
        </div>
    );
}
