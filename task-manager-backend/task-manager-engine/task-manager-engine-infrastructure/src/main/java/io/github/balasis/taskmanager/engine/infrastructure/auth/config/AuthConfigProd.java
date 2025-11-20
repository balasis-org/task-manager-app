package io.github.balasis.taskmanager.engine.infrastructure.auth.config;

import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Getter
@Configuration
@Profile({"prod-azuresql", "prod-h2"})
public class AuthConfigProd implements AuthConfig {

    private final SecretClientProvider secretClientProvider;

    private final String clientId;
    private final String tenantId;
    private final String redirectUri;
    private final String scope = "openid profile email";
    private final String clientSecret;

    public AuthConfigProd(SecretClientProvider secretClientProvider) {
        this.secretClientProvider = secretClientProvider;
        this.clientId = secretClientProvider.getSecret("TASKMANAGER-AUTH-CLIENT-ID");
        this.tenantId = secretClientProvider.getSecret("TASKMANAGER-AUTH-TENANT-ID");
        this.redirectUri = secretClientProvider.getSecret("TASKMANAGER-AUTH-REDIRECT-URI");
        this.clientSecret = secretClientProvider.getSecret("TASKMANAGER-AUTH-CLIENT-SECRET");
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.com/" + tenantId;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return getAuthority() + "/oauth2/v2.0/authorize";
    }
}
