package io.github.balasis.taskmanager.context.web.auth;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.model.RefreshToken;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.jwt.JwtService;
import io.github.balasis.taskmanager.shared.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.RefreshTokenRepository;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.repository.GroupRepository;
import io.github.balasis.taskmanager.engine.core.service.DefaultImageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

// dev/arena-only fake-login bypass — creates or updates a user directly without
// touching Azure AD. supports plan upgrades/downgrades with 7-day grace period.
// active for: dev-h2, dev-mssql, dev-flyway-mssql, arena-stress, arena-security.
//
// defence-in-depth: when DEV_AUTH_SECRET is set (arena deployments), fake-login
// and dev-unlock require either the X-Dev-Auth-Key header (k6) or a DevGate
// cookie (browser session set via /auth/dev-unlock?key=...). local dev profiles
// leave the env var empty and bypass the gate entirely.
@RestController
@RequiredArgsConstructor
@Profile({"dev-h2", "dev-mssql", "dev-flyway-mssql", "prod-arena-stress", "prod-arena-security"})
@RequestMapping("/auth")
public class DevAuthController extends BaseComponent {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GroupRepository groupRepository;
    private final JwtService jwtService;
    private final DefaultImageService defaultImageService;
    private final PlanLimits planLimits;

    @Value("${app.dev-auth-secret:}")
    private String devAuthSecret;

    private static final String DEV_AUTH_HEADER = "X-Dev-Auth-Key";
    private static final String DEV_GATE_COOKIE = "DevGate";

    private String devGateHmac;

    @PostConstruct
    void init() {
        boolean blank  = devAuthSecret == null || devAuthSecret.isBlank();
        boolean looksLikeKvRef = devAuthSecret != null && devAuthSecret.startsWith("@Microsoft.KeyVault");
        logger.info("DevAuth secret state: blank={}, length={}, looksLikeUnresolvedKvRef={}",
                blank, devAuthSecret == null ? 0 : devAuthSecret.length(), looksLikeKvRef);
        if (!blank) {
            devGateHmac = hmacSha256(devAuthSecret, "devgate-cookie");
        }
    }

    private final long JWT_COOKIE_EXPIRE_IN_SECONDS_TIME = 10 * 60;
    private final long REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME = 24 * 60 * 60;

    public record FakeLoginRequest(String email, String name, String subscriptionPlan) {}

    // --- Dev gate: one-time browser unlock --------------------------------
    // Visit /auth/dev-unlock?key=SECRET once — sets a DevGate cookie so all
    // subsequent browser requests to dev-auth pass without extra headers.
    @GetMapping("/dev-unlock")
    public ResponseEntity<Void> devUnlock(@RequestParam String key, HttpServletRequest request) {
        if (devAuthSecret.isBlank() || !MessageDigest.isEqual(
                devAuthSecret.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8))) {
            logger.warn("dev-unlock REJECTED: secretBlank={}, keyLength={}, secretLength={}",
                    devAuthSecret.isBlank(), key.length(), devAuthSecret.length());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        logger.info("dev-unlock ACCEPTED, setting DevGate cookie (secure={}, path=/)",
                request.isSecure());
        ResponseCookie gate = ResponseCookie.from(DEV_GATE_COOKIE, devGateHmac)
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(Duration.ofHours(12))
                .sameSite("Strict")
                .build();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/login")
                .header(HttpHeaders.SET_COOKIE, gate.toString())
                .build();
    }

    @GetMapping("/dev-auth-available")
    public ResponseEntity<Void> devAuthAvailable() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fake-login")
    public ResponseEntity<Map<String, Object>> fakeLogin(
            @RequestBody FakeLoginRequest request, HttpServletRequest httpRequest) {
        if (!isDevAuthAllowed(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "dev-auth gate: missing or invalid credentials"));
        }
        logger.debug("Dev fake-login endpoint reached");
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

        String tenantId = "dev-fake-tenant";
        String azureKey = "dev-fake:" + email;

        User user = userRepository.findByEmail(email)
            .or(() -> userRepository.findByAzureKey(azureKey))
            .map(existing -> {
                existing.setLastActiveAt(Instant.now());
                if (request.subscriptionPlan() != null && !request.subscriptionPlan().isBlank()) {
                    SubscriptionPlan newPlan = resolveSubscriptionPlan(request.subscriptionPlan());
                    SubscriptionPlan oldPlan = existing.getSubscriptionPlan();
                    if (newPlan != oldPlan) {
                        existing.setSubscriptionPlan(newPlan);
                        if (newPlan.ordinal() < oldPlan.ordinal()) {
                            // downgrade — start 7-day grace period
                            existing.setPreviousPlan(oldPlan);
                            existing.setDowngradeGraceDeadline(Instant.now().plus(Duration.ofDays(7)));
                        } else {
                            // upgrade — clear any active grace period
                            existing.setPreviousPlan(null);
                            existing.setDowngradeGraceDeadline(null);
                        }
                        groupRepository.touchLastChangeByOwnerId(existing.getId(), Instant.now());
                    }
                }
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                if (userRepository.count() >= planLimits.getMaxUsers()) {
                    throw new LimitExceededException("Maximum number of users (" + planLimits.getMaxUsers() + ") reached");
                }
                return userRepository.save(
                    User.builder()
                        .azureKey(azureKey)
                        .tenantId(tenantId)
                        .email(email)
                        .name(name)
                        .isOrg(false)
                        .allowEmailNotification(false)
                        .subscriptionPlan(resolveSubscriptionPlan(request.subscriptionPlan()))
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

    // --- Dev auth gate ---------------------------------------------------
    // Returns true when no secret is configured (local dev) or when the
    // request carries a valid X-Dev-Auth-Key header or DevGate cookie.
    private boolean isDevAuthAllowed(HttpServletRequest request) {
        if (devAuthSecret.isBlank()) return true;
        String header = request.getHeader(DEV_AUTH_HEADER);
        if (header != null && MessageDigest.isEqual(
                devAuthSecret.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))) {
            logger.debug("isDevAuthAllowed: passed via header");
            return true;
        }
        boolean hasCookies = request.getCookies() != null;
        boolean foundGate  = false;
        if (hasCookies) {
            for (Cookie c : request.getCookies()) {
                if (DEV_GATE_COOKIE.equals(c.getName())) {
                    foundGate = true;
                    if (devGateHmac != null && MessageDigest.isEqual(
                            devGateHmac.getBytes(StandardCharsets.UTF_8), c.getValue().getBytes(StandardCharsets.UTF_8))) {
                        logger.debug("isDevAuthAllowed: passed via DevGate cookie");
                        return true;
                    }
                    logger.warn("isDevAuthAllowed: DevGate cookie PRESENT but value mismatch (cookieLen={}, secretLen={})",
                            c.getValue().length(), devAuthSecret.length());
                }
            }
        }
        logger.warn("isDevAuthAllowed: DENIED (hasCookies={}, foundGateCookie={}, hasHeader={})",
                hasCookies, foundGate, header != null);
        return false;
    }

    private String generateRandomRefreshToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private SubscriptionPlan resolveSubscriptionPlan(String raw) {
        if (raw == null || raw.isBlank()) return SubscriptionPlan.FREE;
        try {
            return SubscriptionPlan.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SubscriptionPlan.FREE;
        }
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    private ResponseCookie createJwtCookie(String jwt) {
        return ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(JWT_COOKIE_EXPIRE_IN_SECONDS_TIME)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshCookieValue) {
        return ResponseCookie.from("RefreshKey", refreshCookieValue)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME)
                .sameSite("Strict")
                .build();
    }
}
