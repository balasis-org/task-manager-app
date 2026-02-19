package io.github.balasis.taskmanager.engine.core.config;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Profile("prod-azuresql")
@Configuration
@AllArgsConstructor
public class AzureSqlDataSourceConfig {
    private final SecretClient secretClient;

    @Bean
    public DataSource dataSource() {
        //  fetch secrets from Key Vault

        KeyVaultSecret dbUrlSecret = secretClient.getSecret("TASKMANAGER-AZURE-DB-URL");
        KeyVaultSecret dbUsernameSecret = secretClient.getSecret("TASKMANAGER-AZURE-DB-USERNAME");
        KeyVaultSecret dbPasswordSecret = secretClient.getSecret("TASKMANAGER-AZURE-DB-PASSWORD");

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrlSecret.getValue());
        ds.setUsername(dbUsernameSecret.getValue());
        ds.setPassword(dbPasswordSecret.getValue());
        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        ds.setMaximumPoolSize(4);
        ds.setMinimumIdle(2);
        ds.setIdleTimeout(500_000);
        ds.setMaxLifetime(1_200_000);
        ds.setConnectionTimeout(30_000);

        return ds;
    }
}
