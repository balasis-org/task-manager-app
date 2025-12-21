package io.github.balasis.taskmanager.context.web.controller.auth;

import io.github.balasis.taskmanager.context.web.jwt.JwtService;
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
public class AuthController {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthService authService;

    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        String url = authService.getLoginUrl();
        return ResponseEntity.ok(url);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> body) {
        Map<String,Object> toBeReturned = authService.authenticateThroughAzureCode(body.get("code"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, toBeReturned.get("cookie").toString())
                .body(toBeReturned.get("message") );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

}
