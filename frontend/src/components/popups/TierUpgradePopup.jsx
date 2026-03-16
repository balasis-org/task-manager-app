import { FiCpu, FiLayers, FiX } from "react-icons/fi";
import "@styles/popups/Popup.css";
import "@styles/popups/TierUpgradePopup.css";
import "@styles/pages/TermsOfService.css";

export default function TierUpgradePopup({ onClose }) {
    return (
        <div className="popup-overlay" onMouseDown={onClose}>
            <div
                className="popup-card popup-card-wide tier-upgrade-popup"
                onMouseDown={(e) => e.stopPropagation()}
            >
                <button className="tier-upgrade-close" onClick={onClose} title="Close">
                    <FiX size={18} />
                </button>

                <h2><FiLayers size={18} /> Subscription Plans</h2>
                <p className="tier-upgrade-subtitle">
                    The group owner&rsquo;s plan governs all members of that group.
                </p>

                <div className="legal-warning" style={{ marginBottom: '1em' }}>
                    <strong>Thesis project&nbsp;&mdash;&nbsp;no payment exists.</strong>{" "}
                    The tiers below and all their functionality are{" "}
                    <strong>real and fully implemented</strong>, but no payment
                    gateway is in place. Pricing labels are indicative only.
                    Tiers are assigned by a platform administrator for demonstration
                    purposes. No user is charged, therefore no financial obligation
                    exists.
                </div>

                <div className="tos-tier-table-wrap">
                    <table className="tos-tier-table">
                        <thead>
                            <tr>
                                <th>Limit</th>
                                <th>Free</th>
                                <th>Student</th>
                                <th>Organizer</th>
                                <th>Team</th>
                                <th className="tier-pro-col">
                                    Teams Pro
                                    <span className="tier-ai-chip"><FiCpu size={10} /> AI</span>
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr><td>Indicative price</td><td>$0</td><td>$1.90/mo</td><td>$6.20/mo</td><td>$10/mo</td><td>$20/mo</td></tr>
                            <tr><td>Members / group</td><td>8</td><td>20</td><td>30</td><td>50</td><td>50</td></tr>
                            <tr><td>Groups</td><td>2</td><td>5</td><td>10</td><td>15</td><td>15</td></tr>
                            <tr><td>Tasks per group</td><td>30</td><td>100</td><td>300</td><td>500</td><td>500</td></tr>
                            <tr><td>Creator files / task</td><td>1</td><td>Up to 5</td><td>Up to 8</td><td>Up to 8</td><td>Up to 10</td></tr>
                            <tr><td>Assignee files / task</td><td>2</td><td>Up to 5</td><td>Up to 8</td><td>Up to 8</td><td>Up to 10</td></tr>
                            <tr><td>Max file size</td><td>5 MB</td><td>100 MB</td><td>100 MB</td><td>100 MB</td><td>100 MB</td></tr>
                            <tr><td>Storage budget</td><td>100 MB</td><td>500 MB</td><td>2 GB</td><td>5 GB</td><td>5 GB</td></tr>
                            <tr><td>Download budget / mo</td><td>500 MB</td><td>4 GB</td><td>25 GB</td><td>50 GB</td><td>50 GB</td></tr>
                            <tr><td>Download timeout</td><td>30 s</td><td>60 s</td><td>90 s</td><td>120 s</td><td>120 s</td></tr>
                            <tr><td>Email notifications</td><td>-</td><td>-</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
                            <tr><td>Custom images</td><td>-</td><td>Yes</td><td>Yes</td><td>Yes</td><td>Yes</td></tr>
                            <tr><td>Image uploads / mo</td><td>-</td><td>50</td><td>100</td><td>150</td><td>150</td></tr>
                            <tr className="tier-ai-row">
                                <td><FiCpu size={12} className="tier-ai-inline" /> AI analysis credits / mo</td>
                                <td>-</td><td>-</td><td>-</td><td>-</td><td>8,000</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}
