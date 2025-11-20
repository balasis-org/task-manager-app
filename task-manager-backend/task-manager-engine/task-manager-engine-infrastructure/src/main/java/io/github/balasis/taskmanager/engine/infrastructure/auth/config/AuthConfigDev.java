package io.github.balasis.taskmanager.engine.infrastructure.auth.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Getter
@Configuration
@Profile({"dev-mssql", "dev-h2"})
public class AuthConfigDev implements AuthConfig {

    private final String clientId = System.getenv("client-id");
    private final String tenantId = System.getenv("tenant-id");
    private final String redirectUri = "http://localhost:8081/auth/callback";
    private final String authority = "https://login.microsoftonline.com/" + tenantId;
    private final String scope = "openid profile email";
    private final String clientSecret = System.getenv("auth-client-secret");

    @Override
    public String getAuthorizationEndpoint() {
        return authority + "/oauth2/v2.0/authorize";
    }
}
