import { useContext } from "react";
import { AuthContext } from "@context/AuthContext";
import useAiStatus from "@hooks/useAiStatus";

export default function AiStatusBanner() {
    const { user } = useContext(AuthContext);
    const aiStatus = useAiStatus();

    if (!aiStatus) return null;
    if (user?.subscriptionPlan === "FREE") return null;

    const degraded = aiStatus.contentSafetyDegraded || aiStatus.textAnalyticsDegraded;
    if (!degraded) return null;

    return (
        <div className="ai-status-banner">
            ⚠ AI services are temporarily unavailable. Image moderation and
            comment analysis may be delayed.
        </div>
    );
}
