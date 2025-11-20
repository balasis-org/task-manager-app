package io.github.balasis.taskmanager.engine.infrastructure.auth.service;

import com.nimbusds.jwt.SignedJWT;
import io.github.balasis.taskmanager.engine.infrastructure.auth.config.AuthConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthConfig authConfig;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * Build the Azure AD login URL for redirect.
     */
    public String getLoginUrl() {
        return UriComponentsBuilder.fromHttpUrl(authConfig.getAuthorizationEndpoint())
                .queryParam("client_id", authConfig.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", authConfig.getRedirectUri())
                .queryParam("response_mode", "query")
                .queryParam("scope", authConfig.getScope())
                .queryParam("state", "12345")  // optional, for CSRF protection
                .queryParam("prompt","login")
                .toUriString();
    }

    public Map<String, Object> exchangeCodeForToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", authConfig.getClientId());
        formData.add("client_secret", authConfig.getClientSecret());
        formData.add("scope", authConfig.getScope());
        formData.add("code", code);
        formData.add("redirect_uri", authConfig.getRedirectUri());
        formData.add("grant_type", "authorization_code");
        logger.debug("Token request form data: " + formData);

        return webClient.post()
                .uri("https://login.microsoftonline.com/" + authConfig.getTenantId() + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> new RuntimeException("Azure responded with: " + body)))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    public Map<String, Object> decodeIdToken(String idToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(idToken);
            return jwt.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            throw new RuntimeException("Invalid ID token", e);
        }
    }

}
