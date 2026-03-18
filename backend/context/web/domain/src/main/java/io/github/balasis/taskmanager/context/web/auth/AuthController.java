package io.github.balasis.taskmanager.context.web.auth;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// OAuth2 Authorization Code flow (RFC 6749 §4.1) with Azure AD as the identity provider.
//
// flow:
//   1. GET /auth/login-url → builds the Azure AD /authorize URL with a random CSRF state
//      and sets an httpOnly "oauth_state" cookie.
//   2. browser redirects to Azure AD → user logs in → Azure redirects back with ?code=...&state=...
//   3. POST /auth/exchange → verifies the state cookie matches (CSRF protection), exchanges
//      the authorization code for tokens at Microsoft's /oauth2/v2.0/token endpoint,
//      verifies the id_token JWT signature via JWKS, upserts the user, then sets
//      httpOnly JWT + refresh cookies.
//   4. POST /auth/logout → clears both cookies + deletes the refresh token from the DB.
//
// MessageDigest.isEqual is used for state comparison — it's constant-time to prevent
// timing attacks on the CSRF state.
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController extends BaseComponent {
    private final AuthService authService;

    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl(HttpServletRequest request, HttpServletResponse response) {
        String state = authService.generateRandomRefreshToken(32);
        String url = authService.getLoginUrl(state);
        boolean secure = isHttps(request);

        //(sameSite could be skipped ; the callback page is from my domain to the exchange)
        //(but I guess it's ok to let people auto login if people were here and manually click on link)
        ResponseCookie stateCookie = ResponseCookie.from("oauth_state", state)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(5 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        return ResponseEntity.ok(url);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(
            @RequestBody Map<String, String> body,
            @CookieValue(name = "oauth_state", required = false) String stateCookie,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.verifyState(body.get("state"), stateCookie);
        boolean secure = isHttps(request);

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("oauth_state", "")
                        .httpOnly(true).secure(secure).path("/")
                        .maxAge(0).sameSite("Lax").build().toString());

        Map<String, Object> toBeReturned = authService.authenticateThroughAzureCode(body.get("code"), secure);
        response.addHeader(HttpHeaders.SET_COOKIE, toBeReturned.get("cookie").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, toBeReturned.get("refreshKey").toString());
        return ResponseEntity.ok(toBeReturned.get("message"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "RefreshKey", required = false) String refreshCookieValue,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (refreshCookieValue != null) {
            try {
                String[] parts = refreshCookieValue.split(":", 2);
                if (parts.length == 2) {
                    authService.invalidateRefreshToken(Long.parseLong(parts[0]));
                }
            } catch (Exception ignored) {

            }
        }

        boolean secure = isHttps(request);

        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("jwt", "")
                        .httpOnly(true).secure(secure).path("/")
                        .maxAge(0).sameSite("Strict").build().toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("RefreshKey", "")
                        .httpOnly(true).secure(secure).path("/")
                        .maxAge(0).sameSite("Strict").build().toString());

        return ResponseEntity.noContent().build();
    }

    private boolean isHttps(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

}
