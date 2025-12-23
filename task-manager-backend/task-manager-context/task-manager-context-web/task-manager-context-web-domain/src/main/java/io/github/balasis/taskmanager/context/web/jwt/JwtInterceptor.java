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

import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class JwtInterceptor extends BaseComponent implements HandlerInterceptor {

    private final CurrentUser currentUser;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

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

        Long refreshTokenId = Long.parseLong(parts[0]);
        String refreshCode = parts[1];

        var refreshToken = fetchRefreshToken(refreshTokenId);
        verifyRefreshToken(refreshCode, refreshToken);

        String newRefreshCode = generateRandomRefreshToken(32);
        refreshToken.setRefreshCode(newRefreshCode);
        refreshTokenRepository.save(refreshToken);

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

    private void verifyRefreshToken(String refreshCode, RefreshToken refreshToken) {
        if (!refreshToken.getRefreshCode().equals(refreshCode)) {
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
                .maxAge(20)
                .sameSite("Strict")
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshCookieValue) {
        return ResponseCookie.from("RefreshKey", refreshCookieValue)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();
    }

    private String generateRandomRefreshToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
