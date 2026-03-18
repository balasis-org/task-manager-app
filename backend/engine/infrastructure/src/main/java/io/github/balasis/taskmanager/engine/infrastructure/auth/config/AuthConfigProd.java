package io.github.balasis.taskmanager.engine.infrastructure.auth.config;

import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// prod: all OAuth params eagerly read from Azure Key Vault at construction.
//
// scope = "openid profile email User.Read":
//   openid  = required for OpenID Connect, gives us the id_token JWT
//   profile = includes name, preferred_username in the token claims
//   email   = includes email claim (needed for user upsert)
//   User.Read = Microsoft Graph permission to read the user's profile photo
//
// authority = https://login.microsoftonline.com/{tenantId} — the Azure AD
// issuer base URL. appending /oauth2/v2.0/authorize or /token gives the
// standard OAuth2 endpoints.
@Getter
@Configuration
@Profile({"prod-azuresql", "prod-h2", "prod-arena-stress", "prod-arena-security"})
public class AuthConfigProd implements AuthConfig {

    private final SecretClientProvider secretClientProvider;

    private final String clientId;
    private final String tenantId;
    private final String redirectUri;
    private final String authority;
    private final String scope;
    private final String clientSecret;

    public AuthConfigProd(SecretClientProvider secretClientProvider) {
        this.secretClientProvider = secretClientProvider;

        this.clientId = secretClientProvider.getSecret("TASKMANAGER-AUTH-CLIENT-ID");
        this.tenantId = secretClientProvider.getSecret("TASKMANAGER-AUTH-TENANT-ID");
        this.redirectUri = secretClientProvider.getSecret("TASKMANAGER-AUTH-REDIRECT-URI");
        this.authority = "https://login.microsoftonline.com/" + tenantId;
        this.scope = "openid profile email User.Read";
        this.clientSecret = secretClientProvider.getSecret("TASKMANAGER-AUTH-CLIENT-SECRET");
    }

    @Override
    public String getAuthorizationEndpoint() {
        return authority + "/oauth2/v2.0/authorize";
    }
}
