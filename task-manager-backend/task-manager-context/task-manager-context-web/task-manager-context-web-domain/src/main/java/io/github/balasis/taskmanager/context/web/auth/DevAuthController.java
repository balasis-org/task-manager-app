package io.github.balasis.taskmanager.context.web.auth;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.model.RefreshToken;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.jwt.JwtService;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.RefreshTokenRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Profile({"dev-h2", "dev-mssql"})
@RequestMapping("/auth")
public class DevAuthController extends BaseComponent {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final DefaultImageService defaultImageService;

    private final long JWT_COOKIE_EXPIRE_IN_SECONDS_TIME = 10 * 60;
    private final long REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME = 24 * 60 * 60;

    public record FakeLoginRequest(String email, String name) {}

    @PostMapping("/fake-login")
    public ResponseEntity<Map<String, Object>> fakeLogin(@RequestBody FakeLoginRequest request) {
        System.out.println("reached inside");
        if (request == null || request.email() == null || request.email().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email is required"));
        }

        String email = request.email().trim();
        String name;
        if (request.name() != null && !request.name().trim().isEmpty()) {
            name = request.name().trim();
        } else {
            int at = email.indexOf('@');
            name = at > 0 ? email.substring(0, at) : email;
        }

        // Keep this deterministic and clearly "fake".
        String tenantId = "dev-fake-tenant";
        String azureKey = "dev-fake:" + email;

        User user = userRepository.findByEmail(email)
            .or(() -> userRepository.findByAzureKey(azureKey))
            .map(existing -> {
                existing.setLastActiveAt(java.time.Instant.now());
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                if (userRepository.count() >= PlanLimits.MAX_USERS) {
                    throw new LimitExceededException("Maximum number of users (" + PlanLimits.MAX_USERS + ") reached");
                }
                return userRepository.save(
                    User.builder()
                        .azureKey(azureKey)
                        .tenantId(tenantId)
                        .email(email)
                        .name(name)
                        .isOrg(false)
                        .allowEmailNotification(false)
                        .defaultImgUrl(defaultImageService.pickRandom(BlobContainerType.PROFILE_IMAGES))
                        .build()
                );
            });

        String refreshCode = generateRandomRefreshToken(32);
        Long refreshTokenId = refreshTokenRepository.save(
            RefreshToken.builder().refreshCode(refreshCode).user(user).build()
        ).getId();

        String refreshCookieValue = refreshTokenId + ":" + refreshCode;

        var jwtCookie = createJwtCookie(jwtService.generateToken(user.getId().toString()));
        var refreshCookie = createRefreshCookie(refreshCookieValue);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(Map.of(
                "message", "Fake login successful",
                "userId", user.getId(),
                "email", user.getEmail(),
                "name", user.getName()
            ));
    }

        private String generateRandomRefreshToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

    private ResponseCookie createJwtCookie(String jwt) {
        return ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(JWT_COOKIE_EXPIRE_IN_SECONDS_TIME)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshCookieValue) {
        return ResponseCookie.from("RefreshKey", refreshCookieValue)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME)
                .sameSite("Strict")
                .build();
    }
}
