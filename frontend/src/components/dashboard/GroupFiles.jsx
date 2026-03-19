import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FiCheck, FiAlertTriangle, FiFile } from "react-icons/fi";
import { apiGet } from "@assets/js/apiClient.js";
import { formatFileSize } from "@assets/js/fileUtils";
import Spinner from "@components/Spinner";
import "@styles/dashboard/GroupFiles.css";

const STATE_LABELS = {
    TODO: "To do",
    IN_PROGRESS: "In progress",
    TO_BE_REVIEWED: "To be reviewed",
    DONE: "Done",
};

// flat table of all files across all tasks in a group.
// uses short-key DTOs (f.i=id, f.n=name, f.ft=fileType, f.fs=fileSize,
// f.rv=reviews, f.tn=taskTitle, f.ts=taskState, f.un=uploaderName).
// clicking a row navigates to the parent task.
export default function GroupFiles({ groupId }) {
    const navigate = useNavigate();
    const [files, setFiles] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!groupId) return;
        setLoading(true);
        setError(null);
        apiGet(`/api/groups/${groupId}/files`)
            .then((data) => setFiles(data || []))
            .catch(() => setError("Failed to load files"))
            .finally(() => setLoading(false));
    }, [groupId]);

    if (loading) return <Spinner />;
    if (error) return <p className="gf-error">{error}</p>;
    if (files.length === 0) return <p className="gf-empty">No files in this group yet.</p>;

    return (
        <div className="gf-table-wrap">
            <table className="gf-table">
                <thead>
                    <tr>
                        <th>File</th>
                        <th>Type</th>
                        <th>Size</th>
                        <th>Uploader</th>
                        <th>Task</th>
                        <th>State</th>
                        <th>Review</th>
                    </tr>
                </thead>
                <tbody>
                    {files.map((f) => {
                        const latest = f.rv?.length ? f.rv[f.rv.length - 1] : null;
                        return (
                            <tr key={`${f.ft}-${f.i}`}>
                                <td className="gf-cell-name" title={f.n}>
                                    <FiFile size={13} className="gf-file-icon" />
                                    {f.n}
                                </td>
                                <td>{f.ft === "CREATOR" ? "Creator" : "Assignee"}</td>
                                <td>{f.fs != null ? formatFileSize(f.fs) : "—"}</td>
                                <td>{f.ubn || "—"}</td>
                                <td>
                                    <button
                                        className="gf-task-link"
                                        onClick={() => navigate(`/group/${groupId}/task/${f.ti}`)}
                                        title={f.tt}
                                    >
                                        {f.tt || `Task #${f.ti}`}
                                    </button>
                                </td>
                                <td>
                                    <span className={`gf-state gf-state-${(f.ts || "").toLowerCase().replace(/_/g, "-")}`}>
                                        {STATE_LABELS[f.ts] || f.ts}
                                    </span>
                                </td>
                                <td>
                                    {latest ? (
                                        <span
                                            className={`file-review-badge ${latest.status === "CHECKED" ? "badge-checked" : "badge-revision"}`}
                                            title={`${latest.status === "CHECKED" ? "Checked" : "Needs revision"} by ${latest.reviewerName || "reviewer"}${latest.note ? `: ${latest.note}` : ""}`}
                                        >
                                            {latest.status === "CHECKED"
                                                ? <><FiCheck size={10} /> OK</>
                                                : <><FiAlertTriangle size={10} /> Rev</>}
                                        </span>
                                    ) : (
                                        <span className="gf-no-review">—</span>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
