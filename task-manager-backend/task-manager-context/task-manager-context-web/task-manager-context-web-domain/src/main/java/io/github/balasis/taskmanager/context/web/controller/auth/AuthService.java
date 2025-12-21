package io.github.balasis.taskmanager.context.web.controller.auth;

import com.nimbusds.jwt.SignedJWT;
import io.github.balasis.taskmanager.context.base.exception.auth.AuthenticationIntegrityException;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.jwt.JwtService;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.config.AuthConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthConfig authConfig;
    private final WebClient webClient = WebClient.builder().build();
    private final UserRepository userRepository;
    private final JwtService jwtService;

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


    public Map<String,Object> authenticateThroughAzureCode(String code){
        validateAuthorizationCode(code);
        var idToken = extractIdToken(code);
        Map<String, Object> claims = new HashMap<>(decodeIdToken(idToken));
        var tenantId = extractTenantId(claims);
        var email = extractEmail(claims);
        var azureId = (String) claims.get("oid");
        var user = findOrCreateUser(tenantId,email,azureId, (String) claims.get("name"));
        claims.put("userId", user.getId());
        ResponseCookie cookie = createJwtCookie(jwtService.generateToken(user.getAzureKey(), claims));
        return generateResponse(cookie);
    }

    private void validateAuthorizationCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new AuthenticationIntegrityException("Missing authorization code");
        }
    }

    private String extractIdToken(String code) {
        var response = exchangeCodeForToken(code);
        var idToken = (String) response.get("id_token");
        if (idToken == null) {
            throw new AuthenticationIntegrityException("Missing ID token from Azure");
        }
        return idToken;
    }

    private String extractEmail(Map<String, Object> claims){
        var email= (String) claims.get("email");
        if (email == null) {
            throw new AuthenticationIntegrityException("Current microsoft account does not" +
                    "provide an email which is mandatory to login/register in our app");
        }
        return email;
    }

    private String extractTenantId(Map<String, Object> claims){
        var tid = (String) claims.get("tid");
        if (tid == null) {
            throw new AuthenticationIntegrityException("tenantId is missing from claims");
        }
        return tid;
    }

    private User findOrCreateUser(String tenantId,String email, String azureId ,String name){

        boolean isOrg = azureId != null;
        var azureKey = isOrg ? tenantId.concat(azureId) : tenantId;
        var finalName = (name!=null) ? name : email.substring(0,email.indexOf("@"));
        return  userRepository.findByAzureKey(azureKey)
                .orElseGet(() -> {
                    User u = new User();
                    u.setAzureKey(azureKey);
                    u.setTenantId(tenantId);
                    u.setOrg(isOrg);
                    u.setEmail(email);
                    u.setName(finalName);
                    return userRepository.save(u);
                });

    }

    private ResponseCookie createJwtCookie(String jwt){
       return ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();
    }

    private Map<String,Object> generateResponse(ResponseCookie cookie){
        Map<String,String> messageReturned = Map.of("message","Logged in successfully" );
        Map <String,Object> responseItem = new HashMap<>();
        responseItem.put("cookie",cookie);
        responseItem.put("message", messageReturned);
        return responseItem;
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
