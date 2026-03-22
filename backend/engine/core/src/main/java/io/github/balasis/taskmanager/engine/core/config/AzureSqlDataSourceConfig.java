package io.github.balasis.taskmanager.engine.core.config;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

// prod-only datasource that pulls DB credentials from Azure Key Vault at startup.
// dev profiles (dev-h2, dev-postgres) use Spring Boot auto-config instead, so this
// class never loads locally. the pool sizes are intentionally small — Azure SQL Basic
// has a 30-connection cap and we share it across replicas.
@Profile({"prod-azuresql", "prod-arena-stress", "prod-arena-security"})
@Configuration
public class AzureSqlDataSourceConfig {

    private final SecretClient secretClient;

    @Value("${app.hikari.maximum-pool-size:4}")
    private int maxPoolSize;

    @Value("${app.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${app.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    public AzureSqlDataSourceConfig(SecretClient secretClient) {
        this.secretClient = secretClient;
    }

    @Bean
    public DataSource dataSource() {
        // secrets are fetched once on bean creation, not on every connection.
        // if the credentials rotate, the app needs a restart.
        KeyVaultSecret dbUrlSecret = secretClient.getSecret("TASKMANAGER-AZURE-DB-URL");
        KeyVaultSecret dbUsernameSecret = secretClient.getSecret("TASKMANAGER-AZURE-DB-USERNAME");
        KeyVaultSecret dbPasswordSecret = secretClient.getSecret("TASKMANAGER-AZURE-DB-PASSWORD");

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrlSecret.getValue());
        ds.setUsername(dbUsernameSecret.getValue());
        ds.setPassword(dbPasswordSecret.getValue());
        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        ds.setMaximumPoolSize(maxPoolSize);
        ds.setMinimumIdle(minIdle);
        ds.setIdleTimeout(300_000);
        ds.setMaxLifetime(1_800_000);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setLeakDetectionThreshold(60_000);

        return ds;
    }
}
