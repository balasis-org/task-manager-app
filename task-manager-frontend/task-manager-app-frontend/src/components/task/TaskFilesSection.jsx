// Shared between creator files and assignee files - the parent
// just passes different labels, limits, and handlers.
import { useRef, useState } from "react";
import { FiPlus, FiDownload, FiTrash2 } from "react-icons/fi";
import { getFileIcon, formatFileSize } from "@assets/js/fileUtils";
import "@styles/task/TaskFilesSection.css";

export default function TaskFilesSection({
    files = [],
    maxFiles,
    canUpload,
    label,
    emptyText,
    className,
    onFileAdd,
    onDownload,
    onDelete,
    downloadingId,
}) {
    const inputRef = useRef(null);
    const [dragOver, setDragOver] = useState(false);

    const count = files.length;
    const canAdd = canUpload && count < maxFiles;

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

                                {f.fileSize != null && (
                                    <span className="task-file-size" title={`${f.fileSize} bytes`}>
                                        {formatFileSize(f.fileSize)}
                                    </span>
                                )}

                                {downloadingId === f.id && (
                                    <span className="task-file-downloading">↓</span>
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
