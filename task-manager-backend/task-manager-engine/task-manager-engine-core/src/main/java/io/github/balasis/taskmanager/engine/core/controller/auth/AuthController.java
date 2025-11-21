package io.github.balasis.taskmanager.engine.core.controller.auth;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.jwt.JwtService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
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
public class AuthController {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping("/login-url")
    public ResponseEntity<String> getLoginUrl() {
        String url = authService.getLoginUrl();
        return ResponseEntity.ok(url);
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> body) {
        logger.debug("visited");

        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing authorization code");
        }

        // Exchange authorization code for tokens
        Map<String, Object> tokenResponse = authService.exchangeCodeForToken(code);

        String idToken = (String) tokenResponse.get("id_token");
        if (idToken == null) {
            return ResponseEntity.status(500).body("Missing ID token from Azure");
        }

        Map<String, Object> claims = authService.decodeIdToken(idToken);

        String azureId = (String) claims.get("oid");
        if (azureId == null) {
            azureId = (String) claims.get("sub");
        }

        String email = (String) claims.get("email");
        if (email == null) {
            email = (String) claims.get("preferred_username");
        }

        String name = (String) claims.get("name");
        String finalAzureId = azureId;
        String finalEmail = email;
        User user = userRepository.findByAzureId(azureId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setAzureId(finalAzureId);
                    u.setEmail(finalEmail);
                    u.setName(name);
                    return userRepository.save(u);
                });

        String jwt = jwtService.generateToken(user.getAzureId(), claims);

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged in successfully"));
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
