package io.github.balasis.taskmanager.context.web.jwt;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService extends BaseComponent {
    private final SecretClientProvider secretClientProvider;
    private static final long EXPIRATION_MS = 1000L * 60 * 15;

    private SecretKey cachedSignInKey;

    @PostConstruct
    void initSignInKey() {
        String secret_key = secretClientProvider.getSecret("TASKMANAGER-JWT-SECRET");
        byte[] bytes = Base64.getDecoder().decode(secret_key.getBytes(StandardCharsets.UTF_8));
        cachedSignInKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    private SecretKey getSignInKey() {
        return cachedSignInKey;
    }

    public String generateToken(String subject) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(getSignInKey());
        return builder.compact();
    }

    /**
     * Parses and validates the JWT (signature + expiration).
     * jjwt already rejects expired tokens with {@link ExpiredJwtException},
     * so no manual expiration check is needed.
     *
     * Both {@link ExpiredJwtException} and any other {@link JwtException}
     * (malformed, bad signature, etc.) are converted to
     * {@link UnauthenticatedException} so the caller ({@link JwtInterceptor})
     * can catch it uniformly and attempt a refresh-token flow.
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new UnauthenticatedException("JWT token expired");
        } catch (JwtException e) {
            throw new UnauthenticatedException("Invalid JWT token");
        }
    }

    public Claims validateAndExtractClaims(String token) {
        return extractAllClaims(token);
    }

}
