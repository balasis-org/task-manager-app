import { useState } from "react";
import { FiSearch, FiShield, FiAlertTriangle } from "react-icons/fi";
import { apiPatch } from "@assets/js/apiClient";
import { useToast } from "@context/ToastContext";
import { useBlobUrl } from "@context/BlobSasContext";
import "@styles/groupsettings/GsTransferSection.css";

export default function GsTransferSection({ group, members, user, onClose }) {
    const showToast = useToast();
    const blobUrl = useBlobUrl();

    const [showTransfer, setShowTransfer] = useState(false);
    const [transferSearch, setTransferSearch] = useState("");
    const [transferTarget, setTransferTarget] = useState(null);
    const [confirmTransfer, setConfirmTransfer] = useState(false);
    const [transferring, setTransferring] = useState(false);

    // only show other members — can't transfer to yourself
    const otherMembers = (members || []).filter((m) => m.user?.id !== user?.id);
    const filteredTransfer = otherMembers.filter((m) => {
        if (!transferSearch.trim()) return true;
        const q = transferSearch.toLowerCase();
        return (
            (m.user?.name || "").toLowerCase().includes(q) ||
            (m.user?.email || "").toLowerCase().includes(q)
        );
    });

    async function handleTransfer() {
        if (!transferTarget) return;
        setTransferring(true);
        try {
            await apiPatch(
                `/api/groups/${group.id}/groupMembership/${transferTarget.id}/role?role=GROUP_LEADER`
            );
            showToast("Leadership transferred!", "success");
            onClose();
        } catch (err) {
            showToast(err?.message || "Failed to transfer leadership");
        } finally {
            setTransferring(false);
        }
    }

    return (
        <section className="gs-section gs-section-warning">
            <div className="gs-section-header">
                <span className="gs-section-label">
                    <FiShield size={14} className="gs-icon-warning" /> Transfer leadership
                </span>
                {!showTransfer && (
                    <button className="gs-edit-btn gs-warning-btn" onClick={() => setShowTransfer(true)}>
                        Change
                    </button>
                )}
            </div>

            {showTransfer && (
                <div className="gs-transfer-area">
                    <div className="gs-transfer-search">
                        <FiSearch size={13} />
                        <input
                            type="text"
                            value={transferSearch}
                            onChange={(e) => setTransferSearch(e.target.value)}
                            placeholder="Search members…"
                        />
                    </div>
                    {/* inline style below is intentional — not worth a class for a single empty-state line */}
                    <div className="gs-transfer-list">
                        {filteredTransfer.length === 0 ? (
                            <div className="muted" style={{ padding: "8px", fontSize: "13px" }}>No members found</div>
                        ) : (
                            filteredTransfer.map((m) => (
                                <div
                                    key={m.id}
                                    className={`gs-transfer-item${transferTarget?.id === m.id ? " selected" : ""}`}
                                    onClick={() => { setTransferTarget(m); setConfirmTransfer(false); }}
                                >
                                    <img
                                        src={m.user?.imgUrl ? blobUrl(m.user.imgUrl) : (m.user?.defaultImgUrl ? blobUrl(m.user.defaultImgUrl) : "")}
                                        alt=""
                                        className="topbar-member-img"
                                    />
                                    <span>{m.user?.name || m.user?.email}</span>
                                    <span className="topbar-member-role">{m.role}</span>
                                </div>
                            ))
                        )}
                    </div>

                    {transferTarget && !confirmTransfer && (
                        <div className="gs-field-actions">
                            <button className="gs-danger-btn" onClick={() => setConfirmTransfer(true)}>
                                Transfer to {transferTarget.user?.name || transferTarget.user?.email}
                            </button>
                            <button className="gs-cancel-btn" onClick={() => { setShowTransfer(false); setTransferTarget(null); }}>
                                Cancel
                            </button>
                        </div>
                    )}

                    {confirmTransfer && (
                        <div className="gs-confirm-box">
                            <FiAlertTriangle size={16} className="gs-icon-danger" />
                            <span>
                                Are you sure? You will lose your leader role.
                            </span>
                            <div className="gs-field-actions">
                                <button className="gs-danger-btn" onClick={handleTransfer} disabled={transferring}>
                                    {transferring ? "Transferring…" : "Yes, transfer"}
                                </button>
                                <button className="gs-cancel-btn" onClick={() => setConfirmTransfer(false)}>
                                    No
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </section>
    );
}
