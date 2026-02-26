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

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController extends BaseComponent {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthService authService;

    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl(HttpServletRequest request, HttpServletResponse response) {
        String state = authService.generateRandomRefreshToken(32);
        String url = authService.getLoginUrl(state);
        boolean secure = isHttps(request);

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

        // Clear the state cookie
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

        // Invalidate refresh token in DB (best-effort)
        if (refreshCookieValue != null) {
            try {
                String[] parts = refreshCookieValue.split(":", 2);
                if (parts.length == 2) {
                    authService.invalidateRefreshToken(Long.parseLong(parts[0]));
                }
            } catch (Exception ignored) {
                // Cookie may already be invalid or expired
            }
        }

        boolean secure = isHttps(request);

        // Clear cookies with SameSite (ResponseCookie instead of servlet Cookie)
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

    /** True when the original client request was HTTPS (direct or via reverse proxy). */
    private boolean isHttps(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

}
