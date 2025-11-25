package io.github.balasis.taskmanager.context.web.jwt;

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
import java.util.Map;

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

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_MS))
                .signWith(getSignInKey());

        extraClaims.forEach(builder::claim);

        return builder.compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Object getClaim(String token, String claimKey) {
        return extractAllClaims(token).get(claimKey);
    }

    public boolean isTokenValid(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration().after(new Date());
    }

    public String getSubject(String token) {
        return extractAllClaims(token).getSubject();
    }
}
