package io.github.balasis.taskmanager.engine.infrastructure.auth.config;

public interface AuthConfig {

    String getClientId();

    String getTenantId();

    String getRedirectUri();

    String getAuthority();

    String getAuthorizationEndpoint(); // usually authority + "/oauth2/v2.0/authorize"

    String getScope(); // e.g., "openid profile email"

    String getClientSecret();
}
