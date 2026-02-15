package io.github.balasis.taskmanager.context.web.auth;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<String> getLoginUrl() {
        String url = authService.getLoginUrl();
        return ResponseEntity.ok(url);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(
            @RequestBody Map<String, String> body,
            HttpServletResponse response
    ) {
        Map<String, Object> toBeReturned = authService.authenticateThroughAzureCode(body.get("code"));
        response.addHeader(HttpHeaders.SET_COOKIE, toBeReturned.get("cookie").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, toBeReturned.get("refreshKey").toString());
        return ResponseEntity.ok(toBeReturned.get("message"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        Cookie refreshCookie = new Cookie("RefreshKey", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);

        response.addCookie(refreshCookie);

        return ResponseEntity.noContent().build();
    }

}
