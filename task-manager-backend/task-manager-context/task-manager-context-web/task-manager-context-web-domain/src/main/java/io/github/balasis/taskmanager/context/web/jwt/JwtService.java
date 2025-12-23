package io.github.balasis.taskmanager.context.web.jwt;

import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.engine.infrastructure.secret.SecretClientProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
@AllArgsConstructor
public class JwtService {
    private final SecretClientProvider secretClientProvider;
    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24 hours

    private SecretKey getSignInKey() {
        String secret_key = secretClientProvider.getSecret("TASKMANAGER-JWT-SECRET");
        byte[] bytes = Base64.getDecoder().decode(secret_key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(bytes, "HmacSHA256");
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
