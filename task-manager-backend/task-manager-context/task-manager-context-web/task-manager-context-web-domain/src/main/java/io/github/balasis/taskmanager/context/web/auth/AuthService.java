package io.github.balasis.taskmanager.context.web.auth;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.auth.AuthenticationIntegrityException;
import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.model.RefreshToken;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.jwt.JwtService;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.RefreshTokenRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.config.AuthConfig;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService extends BaseComponent {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthConfig authConfig;
    private final WebClient webClient = WebClient.builder().build();
    private final SecretClientProvider secretClientProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final DefaultImageService defaultImageService;
    private final long JWT_COOKIE_EXPIRE_IN_SECONDS_TIME = 10 * 60;
    private final long REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME = 24 * 60 * 60;

    private volatile JWKSet cachedJwkSet;
    private volatile long jwkSetFetchedAt;
    private static final long JWKS_CACHE_MS = 1000L * 60 * 60;

    private String cachedAdminEmail;

    @jakarta.annotation.PostConstruct
    void initAdminEmail() {
        cachedAdminEmail = secretClientProvider.getSecret("ADMIN-EMAIL");
    }

    public String getLoginUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(authConfig.getAuthorizationEndpoint())
                .queryParam("client_id", authConfig.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", authConfig.getRedirectUri())
                .queryParam("response_mode", "query")
                .queryParam("scope", authConfig.getScope())
                .queryParam("state", state)
                .queryParam("prompt","login")
                .toUriString();
    }

    public void verifyState(String stateFromRequest, String stateFromCookie) {
        if (stateFromRequest == null || stateFromCookie == null
                || !MessageDigest.isEqual(
                        stateFromRequest.getBytes(StandardCharsets.UTF_8),
                        stateFromCookie.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthenticationIntegrityException("Invalid OAuth state parameter");
        }
    }

    public void invalidateRefreshToken(Long refreshTokenId) {
        refreshTokenRepository.deleteById(refreshTokenId);
    }

    public Map<String,Object> authenticateThroughAzureCode(String code, boolean secureCookies){
        validateAuthorizationCode(code);
        var idToken = extractIdToken(code);
        Map<String, Object> claims = new HashMap<>(decodeIdToken(idToken));
        var tenantId = extractTenantId(claims);
        var email = extractEmail(claims);
        var azureId = (String) claims.get("oid");
        var user = findOrCreateUser(tenantId,email,azureId, (String) claims.get("name"));
        var refreshCode = generateRandomRefreshToken(32);
        var refreshTokenId = refreshTokenRepository.save(
                RefreshToken.builder().refreshCode(refreshCode).user(user).build()
        ).getId();
        var createRefreshCookieValue = refreshTokenId + ":" + refreshCode;
        var cookie = createJwtCookie(jwtService.generateToken(user.getId().toString()), secureCookies);
        var refreshCookie =  createRefreshCookie(createRefreshCookieValue, secureCookies);
        return generateResponse(cookie, refreshCookie);
    }

    public String generateRandomRefreshToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

        boolean isOrg = !"consumers".equalsIgnoreCase(tenantId);
        if (azureId == null || azureId.isBlank()) {
            throw new AuthenticationIntegrityException("Missing Azure object id (oid) in token");
        }
        var azureKey = tenantId.concat(azureId);
        var finalName = (name!=null) ? name : email.substring(0,email.indexOf("@"));

        String adminEmail = cachedAdminEmail;
        boolean isAdmin = adminEmail != null && adminEmail.equalsIgnoreCase(email);

        return  userRepository.findByAzureKey(azureKey)
                .map(existing -> {
                    existing.setLastActiveAt(java.time.Instant.now());

                    if (isAdmin && existing.getSystemRole() != SystemRole.ADMIN) {
                        existing.setSystemRole(SystemRole.ADMIN);
                    }
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {

                    if (!isAdmin && userRepository.count() >= PlanLimits.MAX_USERS) {
                        throw new LimitExceededException(
                                "We apologize but currently the application has reached the maximum number of user");
                    }
                    User u = new User();
                    u.setAzureKey(azureKey);
                    u.setTenantId(tenantId);
                    u.setDefaultImgUrl(defaultImageService.pickFirst(BlobContainerType.PROFILE_IMAGES));
                    u.setOrg(isOrg);
                    u.setEmail(email);
                    u.setName(finalName);
                    if (isAdmin) {
                        u.setSystemRole(SystemRole.ADMIN);
                    }
                    return userRepository.save(u);
                });

    }

    private ResponseCookie createJwtCookie(String jwt, boolean secure){
       return ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(JWT_COOKIE_EXPIRE_IN_SECONDS_TIME)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie createRefreshCookie(String createRefreshCookieValue, boolean secure){
        return ResponseCookie.from("RefreshKey" , createRefreshCookieValue)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME)
                .sameSite("Strict")
                .build();
    }

    private Map<String,Object> generateResponse(ResponseCookie cookie, ResponseCookie refreshKey){
        Map<String,String> messageReturned = Map.of("message","Logged in successfully" );
        Map <String,Object> responseItem = new HashMap<>();
        responseItem.put("cookie",cookie);
        responseItem.put("refreshKey", refreshKey);
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

            JWKSet jwkSet = getJwkSet();
            String kid = jwt.getHeader().getKeyID();
            JWK jwk = jwkSet.getKeyByKeyId(kid);

            if (jwk == null) {

                cachedJwkSet = null;
                jwkSet = getJwkSet();
                jwk = jwkSet.getKeyByKeyId(kid);
            }
            if (jwk == null) {
                throw new AuthenticationIntegrityException("Unknown signing key in ID token");
            }

            RSAKey rsaKey = jwk.toRSAKey();
            if (!jwt.verify(new RSASSAVerifier(rsaKey))) {
                throw new AuthenticationIntegrityException("ID token signature verification failed");
            }

            var claimsSet = jwt.getJWTClaimsSet();
            Map<String, Object> claims = claimsSet.getClaims();

            if (!claimsSet.getAudience().contains(authConfig.getClientId())) {
                throw new AuthenticationIntegrityException("ID token audience mismatch");
            }

            return claims;
        } catch (AuthenticationIntegrityException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationIntegrityException("ID token verification failed");
        }
    }

    private JWKSet getJwkSet() {
        JWKSet cached = this.cachedJwkSet;
        if (cached != null && (System.currentTimeMillis() - jwkSetFetchedAt) < JWKS_CACHE_MS) {
            return cached;
        }
        try {
            URL jwksUrl = new URL("https://login.microsoftonline.com/"
                    + authConfig.getTenantId() + "/discovery/v2.0/keys");
            cached = JWKSet.load(jwksUrl);
            this.cachedJwkSet = cached;
            this.jwkSetFetchedAt = System.currentTimeMillis();
            return cached;
        } catch (Exception e) {
            throw new AuthenticationIntegrityException("Failed to fetch Azure AD signing keys");
        }
    }

}
