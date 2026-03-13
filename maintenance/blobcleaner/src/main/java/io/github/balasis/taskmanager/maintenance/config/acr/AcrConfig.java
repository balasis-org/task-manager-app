package io.github.balasis.taskmanager.maintenance.config.acr;

import com.azure.containers.containerregistry.ContainerRegistryClient;
import com.azure.containers.containerregistry.ContainerRegistryClientBuilder;
import com.azure.identity.ManagedIdentityCredential;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod-azuresql")
@AllArgsConstructor
public class AcrConfig {

    private final ManagedIdentityCredential managedIdentityCredential;

    @Bean
    public ContainerRegistryClient containerRegistryClient() {
        return new ContainerRegistryClientBuilder()
                .endpoint("https://anstaskmanageracr-g3bwdtdrb8h9bfem.azurecr.io")
                .credential(managedIdentityCredential)
                .buildClient();
    }
}
