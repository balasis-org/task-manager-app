package io.github.balasis.taskmanager.engine.monitoring.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;
import java.util.Set;

/**
 * Custom OTel sampler that drops pure-notifier polling endpoints from telemetry entirely.
 * These are cheap 204/409 endpoints that carry no diagnostic data — they just tell the
 * frontend "something changed" or "nothing changed".
 *
 * All other requests (including the /refresh endpoint which carries real dashboard data)
 * are delegated to the wrapped parent sampler (typically a TraceIdRatio sampler).
 *
 * Dropped endpoints:
 * - /api/group-invitations/check-new       (invite notifier)
 * - /api/groups/{id}/has-changed            (group change notifier)
 * - /api/groups/{id}/task/{id}/has-changed  (task change notifier)
 * - /api/groups/{id}/task/{id}/comments/has-changed  (comment change notifier)
 *
 * How it detects polling endpoints:
 * Spring MVC sets the span name to "HTTP_METHOD route_template" (e.g. "GET /api/groups/{groupId}/has-changed").
 * We match against these span names since the semconv HttpAttributes aren't available as a dependency.
 */
public class PollingEndpointSampler implements Sampler {

    private final Sampler parentSampler;

    // Exact substrings to match inside the span name.
    // Spring MVC span names look like "GET /api/group-invitations/check-new"
    private static final Set<String> EXACT_DROP_ROUTES = Set.of(
            "/api/group-invitations/check-new",
            "/presence"
    );

    // Suffix patterns — if the span name ends with any of these, drop it.
    // Catches /{id}/has-changed and /{id}/comments/has-changed variants.
    private static final Set<String> SUFFIX_DROP_PATTERNS = Set.of(
            "/has-changed"
    );

    public PollingEndpointSampler(Sampler parentSampler) {
        this.parentSampler = parentSampler;
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String spanName,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks
    ) {
        // Only filter SERVER spans (incoming HTTP requests). Let everything else through.
        if (spanKind == SpanKind.SERVER && spanName != null && isPollingEndpoint(spanName)) {
            return SamplingResult.drop();
        }

        return parentSampler.shouldSample(parentContext, traceId, spanName, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "PollingEndpointSampler{parent=" + parentSampler.getDescription() + "}";
    }

    /**
     * Checks if the span name matches a known polling endpoint.
     * Span names from Spring MVC look like "GET /api/groups/{groupId}/has-changed".
     */
    private boolean isPollingEndpoint(String spanName) {
        for (String route : EXACT_DROP_ROUTES) {
            if (spanName.contains(route)) {
                return true;
            }
        }
        for (String suffix : SUFFIX_DROP_PATTERNS) {
            if (spanName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
