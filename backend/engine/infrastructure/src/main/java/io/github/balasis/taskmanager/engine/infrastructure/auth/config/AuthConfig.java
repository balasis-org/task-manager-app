package io.github.balasis.taskmanager.engine.infrastructure.auth.config;

// OAuth2 config for the Azure AD Authorization Code flow.
// provides all the parameters needed to build the /authorize URL and
// exchange the authorization code at the /token endpoint.
// prod reads values from Azure Key Vault; dev reads from plain env vars.
public interface AuthConfig {

    String getClientId();

    String getTenantId();

    String getRedirectUri();

    String getAuthority();

    String getAuthorizationEndpoint();

    String getScope();

    String getClientSecret();
}
