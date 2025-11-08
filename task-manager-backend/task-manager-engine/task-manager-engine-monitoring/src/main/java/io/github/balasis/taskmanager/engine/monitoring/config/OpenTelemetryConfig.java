package io.github.balasis.taskmanager.engine.monitoring.config;

import com.azure.monitor.opentelemetry.autoconfigure.AzureMonitorAutoConfigure;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@AllArgsConstructor
@Configuration
@Profile("OpenTelemetry")
public class OpenTelemetryConfig {
    private final SecretClientProvider secretClientProvider;

    @Bean
    @Profile({"dev-h2","dev-mssql"})
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
        AutoConfiguredOpenTelemetrySdkBuilder otelSdkBuilder = AutoConfiguredOpenTelemetrySdk.builder();
        AzureMonitorAutoConfigure.customize(otelSdkBuilder, connectionString);
        return otelSdkBuilder.build().getOpenTelemetrySdk();
    }
}
