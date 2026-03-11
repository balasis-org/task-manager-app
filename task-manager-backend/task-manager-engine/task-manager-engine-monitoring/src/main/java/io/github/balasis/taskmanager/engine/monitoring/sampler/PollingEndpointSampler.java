package io.github.balasis.taskmanager.engine.monitoring.sampler;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;
import java.util.Set;

// Drops polling/notifier endpoints from OTel telemetry so they dont clutter traces.
// These are cheap 204/409 endpoints (has-changed, check-new, presence) that
// dont carry useful diagnostic info. Everything else gets delegated to the
// parent sampler (usually a ratio sampler).
public class PollingEndpointSampler implements Sampler {

    private final Sampler parentSampler;

    // routes to match inside the span name
    private static final Set<String> EXACT_DROP_ROUTES = Set.of(
            "/api/group-invitations/check-new",
            "/presence"
    );

    // suffix patterns for the various /has-changed variants
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
        // only filter incoming HTTP requests, let internal spans through
        if (spanKind == SpanKind.SERVER && spanName != null && isPollingEndpoint(spanName)) {
            return SamplingResult.drop();
        }

        return parentSampler.shouldSample(parentContext, traceId, spanName, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "PollingEndpointSampler{parent=" + parentSampler.getDescription() + "}";
    }

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
