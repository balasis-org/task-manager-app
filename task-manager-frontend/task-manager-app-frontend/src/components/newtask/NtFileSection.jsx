import { useState, useRef } from "react";
import { FiX, FiPlus } from "react-icons/fi";
import { LIMITS } from "@assets/js/inputValidation";
import { isFileTooLarge, getFileIcon } from "@assets/js/fileUtils";
import "@styles/newtask/NtFileSection.css";

export default function NtFileSection({ files, onFilesChange, onError }) {
    const [fileDragOver, setFileDragOver] = useState(false);
    const fileInputRef = useRef(null);
    const maxFiles = LIMITS.MAX_TASK_FILES;

    function addFiles(incoming) {
        const oversized = incoming.filter(isFileTooLarge);
        if (oversized.length) {
            onError(`File(s) exceed ${LIMITS.MAX_FILE_SIZE_MB} MB limit: ${oversized.map((f) => f.name).join(", ")}`);
            return;
        }
        const combined = [...files, ...incoming];
        if (combined.length > maxFiles) {
            onError(`Maximum ${maxFiles} files allowed.`);
        }
        onFilesChange(combined.slice(0, maxFiles));
    }

    function handleFileAdd(e) {
        const picked = Array.from(e.target.files || []);
        addFiles(picked);
        e.target.value = "";
    }

    function handleFileDrop(e) {
        e.preventDefault();
        setFileDragOver(false);
        const dropped = Array.from(e.dataTransfer.files || []);
        if (!dropped.length) return;
        addFiles(dropped);
    }

    function removeFile(idx) {
        onFilesChange(files.filter((_, i) => i !== idx));
    }

    return (
        <div
            className={`popup-picker-section${fileDragOver ? " file-drag-over" : ""}`}
            onDragOver={(e) => { e.preventDefault(); setFileDragOver(true); }}
            onDragLeave={() => setFileDragOver(false)}
            onDrop={handleFileDrop}
        >
            <span className="popup-picker-label">
                Files ({files.length}/{maxFiles})
                {files.length < maxFiles && (
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
                                <button type="button" className="popup-chip-rm" onClick={() => removeFile(i)}>
                                    <FiX size={10} />
                                </button>
                            </span>
                        );
                    })}
                </div>
            )}

            {files.length < maxFiles && (
                <span className="popup-drop-hint">or drag & drop files here</span>
            )}
        </div>
    );
}
