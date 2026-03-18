package io.github.balasis.taskmanager.engine.monitoring.config;

import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigure;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.github.balasis.taskmanager.engine.monitoring.sampler.PollingEndpointSampler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// configures Azure Monitor OpenTelemetry SDK with a custom sampler that drops
// polling endpoints (has-changed, presence, check-new) before applying a 10%
// ratio sampler to everything else. this keeps App Insights costs down while
// still capturing enough traces for debugging.
// three separate bean methods because each profile reads a different
// Application Insights connection string (dev vs prod-h2 vs prod-azuresql).
@AllArgsConstructor
@Configuration
@Profile("OpenTelemetry")
public class OpenTelemetryConfig {
    private final SecretClientProvider secretClientProvider;

    // Fraction of non-polling requests to sample (0.0–1.0).
    // Tweak this to control telemetry volume / cost. Polling endpoints are always dropped.
    private static final double SAMPLING_RATIO = 0.10;

    @Bean
    @Profile({"dev-h2","dev-mssql","dev-flyway-mssql"})
    public OpenTelemetry openTelemetryDev() {
        return buildOpenTelemetryDev();
    }

    @Bean
    @Profile("prod-h2")
    public OpenTelemetry openTelemetryProdH2() {
        return buildOpenTelemetryProdH2();
    }

    @Bean
    @Profile("prod-azuresql")
    public OpenTelemetry openTelemetryProdAzureSql() {
        return buildOpenTelemetryProdAzureSql();
    }

    private OpenTelemetry buildOpenTelemetryDev() {
        String conn = secretClientProvider.getSecret("APPLICATIONINSIGHTS_CONNECTION_STRING");
        return buildOpenTelemetry(conn);
    }

    private OpenTelemetry buildOpenTelemetryProdH2() {
        String conn = secretClientProvider.getSecret("TASKMANAGER-APP-INSIGHT-CONN-STR-DEV");
        return buildOpenTelemetry(conn);
    }

    private OpenTelemetry buildOpenTelemetryProdAzureSql() {
        String conn = secretClientProvider.getSecret("TASKMANAGER-APP-INSIGHT-CONN-STR-PROD");
        return buildOpenTelemetry(conn);
    }

    private OpenTelemetry buildOpenTelemetry(String connectionString) {
        // Base sampler: only keep SAMPLING_RATIO % of traces
        Sampler ratioSampler = Sampler.traceIdRatioBased(SAMPLING_RATIO);
        // Wrap with our custom filter that drops polling endpoints before ratio sampling
        Sampler finalSampler = new PollingEndpointSampler(ratioSampler);

        AutoConfiguredOpenTelemetrySdkBuilder otelSdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();
        AzureMonitorAutoConfigure.customize(otelSdkBuilder, connectionString);

        // Override the default sampler with our polling-aware one
        otelSdkBuilder.addTracerProviderCustomizer((tracerProviderBuilder, configProperties) ->
                tracerProviderBuilder.setSampler(finalSampler)
        );

        return otelSdkBuilder.build().getOpenTelemetrySdk();
    }
}
