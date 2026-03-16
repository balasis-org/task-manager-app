// Chip-style picker, reused for both reviewers and assignees in the new-task form
import { FiX, FiSearch } from "react-icons/fi";
import userImg from "@assets/js/userImg";
import { formatRole } from "@assets/js/formatLabel";
import "@styles/newtask/NtMemberPicker.css";

export default function NtMemberPicker({
    label,
    selected,
    eligible,
    search,
    onSearchChange,
    onAdd,
    onRemove,
    blobUrl,
}) {
    return (
        <div className="popup-picker-section">
            <span className="popup-picker-label">
                {label}
                <span className="popup-picker-search">
                    <FiSearch size={11} />
                    <input
                        type="text"
                        placeholder="Search…"
                        value={search}
                        onChange={(e) => onSearchChange(e.target.value)}
                    />
                </span>
            </span>

            {selected.length > 0 && (
                <div className="popup-chip-list">
                    {selected.map((m) => (
                        <span key={m.user?.id} className="popup-chip" title={m.user?.email}>
                            <img src={userImg(m.user, blobUrl)} alt="" className="popup-chip-img" />
                            {m.user?.name || m.user?.email}
                            <button type="button" className="popup-chip-rm" onClick={() => onRemove(m.user?.id)}>
                                <FiX size={10} />
                            </button>
                        </span>
                    ))}
                </div>
            )}

            {eligible.length > 0 && (
                <div className="popup-picker-dropdown">
                    {eligible.map((m) => (
                        <div key={m.user?.id} className="popup-picker-item" onClick={() => onAdd(m)}>
                            <img src={userImg(m.user, blobUrl)} alt="" className="popup-search-img" />
                            <span title={m.user?.email}>{m.user?.name || m.user?.email}</span>
                            <span className="popup-picker-role">{formatRole(m.role)}</span>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
