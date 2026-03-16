import { useState, useEffect, useCallback } from "react";
import { FiCpu } from "react-icons/fi";
import { apiGet, apiPost, apiDelete } from "@assets/js/apiClient.js";
import { formatDateTime } from "@assets/js/formatDate";
import "@styles/comments/AnalysisPanel.css";

const ANALYSIS_TYPES = [
    { value: "FULL",          label: "Full (Analysis + Summary)" },
    { value: "ANALYSIS_ONLY", label: "Analysis only" },
    { value: "SUMMARY_ONLY",  label: "Summary only" },
];

const SENTIMENT_COLORS = {
    POSITIVE: "#22c55e",
    NEUTRAL:  "#64748b",
    NEGATIVE: "#ef4444",
    MIXED:    "#f59e0b",
};

export default function AnalysisPanel({ groupId, taskId, groupDetail, showToast }) {
    const isTeamsPro = groupDetail?.op === "TEAMS_PRO";

    const [estimate, setEstimate] = useState(null);
    const [snapshot, setSnapshot] = useState(null);
    const [analysisType, setAnalysisType] = useState("FULL");
    const [submitting, setSubmitting] = useState(false);
    const [expanded, setExpanded] = useState(false);
    const [showPerComment, setShowPerComment] = useState(false);
    const [bulkConfirm, setBulkConfirm] = useState(false);
    const [bulkDeleting, setBulkDeleting] = useState(false);

    const fetchEstimate = useCallback(async () => {
        if (!isTeamsPro) return;
        try {
            const data = await apiGet(`/api/groups/${groupId}/task/${taskId}/analysis-estimate`);
            setEstimate(data);
        } catch { /* non-critical */ }
    }, [groupId, taskId, isTeamsPro]);

    const fetchSnapshot = useCallback(async () => {
        if (!isTeamsPro) return;
        try {
            const res = await apiGet(`/api/groups/${groupId}/task/${taskId}/analysis`);
            if (res) setSnapshot(res);
        } catch (err) {
            if (err?.status !== 204) { /* ignore no-content */ }
        }
    }, [groupId, taskId, isTeamsPro]);

    useEffect(() => {
        fetchEstimate();
        fetchSnapshot();
    }, [fetchEstimate, fetchSnapshot]);

    if (!isTeamsPro) return null;

    async function handleAnalyze() {
        setSubmitting(true);
        try {
            await apiPost(`/api/groups/${groupId}/task/${taskId}/analyze?type=${analysisType}`);
            showToast("Analysis queued — results will appear shortly.", "success");
            // poll for results after a short delay
            setTimeout(() => { fetchSnapshot(); fetchEstimate(); }, 4000);
            setTimeout(() => fetchSnapshot(), 10000);
        } catch (err) {
            showToast(err?.message || "Failed to start analysis");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleBulkDelete() {
        if (!snapshot?.analyzedAt) return;
        setBulkDeleting(true);
        try {
            const res = await apiDelete(
                `/api/groups/${groupId}/task/${taskId}/comments/bulk?before=${encodeURIComponent(snapshot.analyzedAt)}`
            );
            showToast(`Deleted ${res?.deleted ?? 0} comment(s).`, "success");
            setBulkConfirm(false);
            fetchEstimate();
            fetchSnapshot();
        } catch (err) {
            showToast(err?.message || "Failed to bulk-delete comments");
        } finally {
            setBulkDeleting(false);
        }
    }

    const creditsForType = estimate
        ? analysisType === "ANALYSIS_ONLY" ? (estimate.analysisCredits + estimate.egressCredits)
        : analysisType === "SUMMARY_ONLY"  ? (estimate.summaryCredits + estimate.egressCredits)
        : estimate.fullCredits
        : 0;

    const noComments = estimate?.commentCount === 0;

    return (
        <div className="analysis-panel">
            <button
                className="analysis-panel-toggle"
                onClick={() => setExpanded(v => !v)}
            >
                <FiCpu size={14} className="analysis-panel-icon" />
                <span>Comment Intelligence</span>
                <span className="analysis-toggle-arrow">{expanded ? "▾" : "▸"}</span>
            </button>

            {expanded && (
                <div className="analysis-panel-body">
                    {/* ── Estimate ── */}
                    {estimate && (
                        <div className="analysis-estimate">
                            <div className="analysis-est-row">
                                <span className="analysis-est-label">Comments</span>
                                <span className="analysis-est-value">{estimate.commentCount}</span>
                            </div>
                            <div className="analysis-est-row">
                                <span className="analysis-est-label">Credits for selected type</span>
                                <span className="analysis-est-value">{creditsForType}</span>
                            </div>
                            <div className="analysis-est-row">
                                <span className="analysis-est-label">Budget remaining</span>
                                <span className="analysis-est-value">
                                    {estimate.budgetRemaining.toLocaleString()} / {estimate.budgetMax.toLocaleString()}
                                </span>
                            </div>
                            {estimate.stale && (
                                <div className="analysis-stale-hint">
                                    Estimate may be outdated — new comments added since last estimate.
                                </div>
                            )}
                        </div>
                    )}

                    {/* ── Trigger ── */}
                    <div className="analysis-trigger">
                        <select
                            className="analysis-type-select"
                            value={analysisType}
                            onChange={e => setAnalysisType(e.target.value)}
                        >
                            {ANALYSIS_TYPES.map(t => (
                                <option key={t.value} value={t.value}>{t.label}</option>
                            ))}
                        </select>
                        <button
                            className="analysis-run-btn"
                            onClick={handleAnalyze}
                            disabled={submitting || noComments || creditsForType > (estimate?.budgetRemaining ?? 0)}
                        >
                            {submitting ? "Queuing…" : <><FiCpu size={12} style={{ verticalAlign: '-0.12em', marginRight: '0.3em' }} />Analyze ({creditsForType} cr)</>}
                        </button>
                    </div>
                    {noComments && <p className="analysis-hint">Add comments before running analysis.</p>}
                    {creditsForType > (estimate?.budgetRemaining ?? 0) && !noComments && (
                        <p className="analysis-hint analysis-hint-warn">Not enough credits remaining.</p>
                    )}

                    {/* ── Results: Sentiment ── */}
                    {snapshot?.analyzedAt && (
                        <div className="analysis-results">
                            <h4 className="analysis-section-title">
                                Sentiment Analysis
                                {snapshot.analysisStale && <span className="analysis-stale-tag">stale</span>}
                            </h4>

                            <div className="analysis-sentiment-overview">
                                <div
                                    className="analysis-sentiment-badge"
                                    style={{ borderColor: SENTIMENT_COLORS[snapshot.overallSentiment] || "#64748b" }}
                                >
                                    <span className="analysis-sentiment-label">{snapshot.overallSentiment}</span>
                                    <span className="analysis-sentiment-conf">
                                        {(snapshot.overallConfidence * 100).toFixed(0)}%
                                    </span>
                                </div>
                                <div className="analysis-sentiment-bars">
                                    <SentimentBar label="Positive" count={snapshot.positiveCount} total={snapshot.analysisCommentCount} color="#22c55e" />
                                    <SentimentBar label="Neutral"  count={snapshot.neutralCount}  total={snapshot.analysisCommentCount} color="#64748b" />
                                    <SentimentBar label="Negative" count={snapshot.negativeCount} total={snapshot.analysisCommentCount} color="#ef4444" />
                                </div>
                            </div>

                            {snapshot.piiDetectedCount > 0 && (
                                <div className="analysis-pii-warn">
                                    ⚠ {snapshot.piiDetectedCount} comment(s) with PII detected by AI.
                                </div>
                            )}

                            {/* Key Phrases */}
                            {snapshot.keyPhrases?.length > 0 && (
                                <div className="analysis-keyphrases">
                                    <h5 className="analysis-sub-title">Key Phrases</h5>
                                    <div className="analysis-kp-list">
                                        {snapshot.keyPhrases.map((kp, i) => (
                                            <span key={i} className="analysis-kp-chip">{kp}</span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Per-comment breakdown toggle */}
                            {snapshot.commentResults?.length > 0 && (
                                <div className="analysis-per-comment">
                                    <button
                                        className="analysis-toggle-detail"
                                        onClick={() => setShowPerComment(v => !v)}
                                    >
                                        {showPerComment ? "Hide" : "Show"} per-comment details ({snapshot.commentResults.length})
                                    </button>
                                    {showPerComment && (
                                        <div className="analysis-comment-list">
                                            {snapshot.commentResults.map((cr, i) => (
                                                <div key={i} className="analysis-comment-row">
                                                    <span
                                                        className="analysis-cr-dot"
                                                        style={{ background: SENTIMENT_COLORS[cr.sentiment] || "#64748b" }}
                                                    />
                                                    <span className="analysis-cr-sentiment">{cr.sentiment}</span>
                                                    <span className="analysis-cr-conf">{(cr.confidence * 100).toFixed(0)}%</span>
                                                    {cr.piiEntityCount > 0 && (
                                                        <span className="analysis-cr-pii">PII: {cr.piiEntityCount}</span>
                                                    )}
                                                    {cr.keyPhrases?.length > 0 && (
                                                        <span className="analysis-cr-kps">{cr.keyPhrases.join(", ")}</span>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}

                            <span className="analysis-timestamp">
                                Analyzed {formatDateTime(snapshot.analyzedAt)} · {snapshot.analysisCommentCount} comment(s)
                            </span>
                        </div>
                    )}

                    {/* ── Results: Summary ── */}
                    {snapshot?.summarizedAt && (
                        <div className="analysis-results">
                            <h4 className="analysis-section-title">
                                AI Summary
                                {snapshot.summaryStale && <span className="analysis-stale-tag">stale</span>}
                            </h4>
                            <p className="analysis-summary-text">{snapshot.summaryText}</p>
                            <span className="analysis-timestamp">
                                Summarized {formatDateTime(snapshot.summarizedAt)} · {snapshot.summaryCommentCount} comment(s)
                            </span>
                        </div>
                    )}

                    {/* ── Bulk Delete ── */}
                    {snapshot?.analyzedAt && snapshot.piiDetectedCount > 0 && (
                        <div className="analysis-bulk-section">
                            {!bulkConfirm ? (
                                <button
                                    className="analysis-bulk-btn"
                                    onClick={() => setBulkConfirm(true)}
                                >
                                    Bulk-delete comments before analysis
                                </button>
                            ) : (
                                <div className="analysis-bulk-confirm">
                                    <p>
                                        Delete all comments created before{" "}
                                        <strong>{formatDateTime(snapshot.analyzedAt)}</strong>?
                                        This cannot be undone.
                                    </p>
                                    <div className="analysis-bulk-btns">
                                        <button
                                            className="btn-danger btn-sm"
                                            onClick={handleBulkDelete}
                                            disabled={bulkDeleting}
                                        >
                                            {bulkDeleting ? "Deleting…" : "Confirm delete"}
                                        </button>
                                        <button
                                            className="btn-secondary btn-sm"
                                            onClick={() => setBulkConfirm(false)}
                                        >
                                            Cancel
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

function SentimentBar({ label, count, total, color }) {
    const pct = total > 0 ? (count / total) * 100 : 0;
    return (
        <div className="analysis-sbar">
            <span className="analysis-sbar-label">{label}</span>
            <div className="analysis-sbar-track">
                <div className="analysis-sbar-fill" style={{ width: `${pct}%`, background: color }} />
            </div>
            <span className="analysis-sbar-count">{count}</span>
        </div>
    );
}
