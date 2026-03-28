package io.github.balasis.taskmanager.context.web.jwt;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.context.base.model.RefreshToken;
import io.github.balasis.taskmanager.engine.core.repository.RefreshTokenRepository;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.CurrentUser;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

// auth interceptor: first line of auth for every API request.
//
// refresh token rotation:
//   when the JWT expires (15 min), instead of forcing a re-login, the interceptor
//   reads the "RefreshKey" cookie (format: "tokenId:refreshCode"), looks up the
//   RefreshToken row in the DB, verifies the code with constant-time comparison
//   (MessageDigest.isEqual), then generates a NEW refresh code (rotation), mints
//   a fresh JWT, and sends both back as Set-Cookie headers.
//   why rotate? if an attacker steals the refresh cookie, the real user's next
//   request will fail (code mismatch) → detectable. this is the "refresh token
//   rotation" pattern recommended by OAuth 2.0 for browser clients.
//
// order: runs at interceptor order 1 — before rate-limit and account-ban checks.
// populates CurrentUser (request-scoped bean) with the authenticated userId.
@Component
@RequiredArgsConstructor
public class JwtInterceptor extends BaseComponent implements HandlerInterceptor {

    private final CurrentUser currentUser;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long JWT_COOKIE_EXPIRE_IN_SECONDS_TIME = 10 * 60;
    private final long REFRESH_COOKIE_EXPIRE_IN_SECONDS_TIME = 24 * 60 * 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var cookies = extractCookies(request);
        String jwtToken = cookies[0];
        String refreshCookieValue = cookies[1];

        if (jwtToken != null) {
            try {
                return validateJwt(jwtToken, response);
            } catch (UnauthenticatedException ex) {
                return attemptRefresh(refreshCookieValue, response);
            }
        } else if (refreshCookieValue != null) {
            return attemptRefresh(refreshCookieValue, response);
        }

        throw new UnauthenticatedException("No valid JWT or refresh token provided");
    }

    private boolean validateJwt(String jwtToken, HttpServletResponse response) {
        var claims = jwtService.validateAndExtractClaims(jwtToken);
        currentUser.setUserId(Long.parseLong(claims.getSubject()));
        response.addHeader("Set-Cookie", createJwtCookie(jwtToken).toString());
        return true;
    }

    private boolean attemptRefresh(String refreshCookieValue, HttpServletResponse response) {
        if (refreshCookieValue == null) throw new UnauthenticatedException("Refresh token missing");

        String[] parts = refreshCookieValue.split(":", 2);
        if (parts.length != 2) throw new UnauthenticatedException("Invalid refresh cookie format");

        long refreshTokenId;
        try { refreshTokenId = Long.parseLong(parts[0]); }
        catch (NumberFormatException e) { throw new UnauthenticatedException("Invalid refresh token format"); }
        String refreshCode = parts[1];

        var refreshToken = fetchRefreshToken(refreshTokenId);
        verifyRefreshToken(refreshCode, refreshToken);

        if (refreshToken.getExpiresAt() != null
                && refreshToken.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new UnauthenticatedException("Refresh token expired");
        }

        String newRefreshCode = generateRandomRefreshToken(32);
        refreshToken.setRefreshCode(sha256Hex(newRefreshCode));
        try {
            refreshTokenRepository.save(refreshToken);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new UnauthenticatedException("Refresh token already rotated");
        }

        var user = refreshToken.getUser();
        var newJwt = jwtService.generateToken(user.getId().toString());
        response.addHeader("Set-Cookie", createJwtCookie(newJwt).toString());

        String newRefreshCookieValue = refreshToken.getId() + ":" + newRefreshCode;
        response.addHeader("Set-Cookie", createRefreshCookie(newRefreshCookieValue).toString());

        currentUser.setUserId(user.getId());
        return true;
    }

    private RefreshToken fetchRefreshToken(Long refreshTokenId) {
        return refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new UnauthenticatedException("Invalid refresh token"));
    }

    private void verifyRefreshToken(String rawCode, RefreshToken refreshToken) {
        if (!MessageDigest.isEqual(
                sha256Hex(rawCode).getBytes(StandardCharsets.UTF_8),
                refreshToken.getRefreshCode().getBytes(StandardCharsets.UTF_8))) {
            refreshTokenRepository.deleteById(refreshToken.getId());
            throw new UnauthenticatedException("Refresh token mismatch");
        }
    }

    private String[] extractCookies(HttpServletRequest request) {
        String jwtToken = null;
        String refreshCookieValue = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) jwtToken = cookie.getValue();
                if ("RefreshKey".equals(cookie.getName())) refreshCookieValue = cookie.getValue();
            }
        }
        return new String[]{jwtToken, refreshCookieValue};
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

    private String generateRandomRefreshToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
