package io.github.balasis.taskmanager.engine.infrastructure.auth.config;

public interface AuthConfig {

    String getClientId();

    String getTenantId();

    String getRedirectUri();

    String getAuthority();

    String getAuthorizationEndpoint();

    String getScope();

    String getClientSecret();
}
