package io.github.balasis.taskmanager.context.web.jwt;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.jsonwebtoken.Claims;
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
    private static final long EXPIRATION_MS = 1000L * 60 * 15; // 15 minutes (matches cookie maxAge)

    /** Cached at startup — avoids a Key Vault round-trip on every request. */
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

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims validateAndExtractClaims(String token) {
        Claims claims = extractAllClaims(token);
        if (claims.getExpiration().before(new Date())) {
            throw new UnauthenticatedException("JWT token expired");
        }
        return claims;
    }

}
