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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

// Internal JWT (JSON Web Token) minting + validation.
//
// uses HMAC-SHA256 (HS256): a symmetric signing algorithm where the same secret key
// signs and verifies the token. the key is a Base64-encoded 256-bit secret stored in
// Azure Key Vault (or an env var in dev). SecretKeySpec wraps it as a JCA key object.
//
// token structure: header.payload.signature (Base64url-encoded, dot-separated).
//   - header: {"alg":"HS256"}
//   - payload: {"sub":"userId", "iat":..., "exp":...}  (15 min expiry)
//   - signature: HMAC-SHA256(header + "." + payload, secret)
//
// Jwts.builder() creates the token; Jwts.parser().verifyWith() validates it.
// expired tokens throw ExpiredJwtException → mapped to UnauthenticatedException.
@Service
@RequiredArgsConstructor
public class JwtService extends BaseComponent {
    private final SecretClientProvider secretClientProvider;

    @Value("${app.jwt-secret-name:TASKMANAGER-JWT-SECRET}")
    private String jwtSecretName;

    private static final long EXPIRATION_MS = 1000L * 60 * 15;

    private SecretKey cachedSignInKey;

    @PostConstruct
    void initSignInKey() {
        // the secret is stored Base64-encoded in Key Vault (or env var).
        // we decode it to raw bytes, then wrap as a JCA SecretKeySpec with the
        // "HmacSHA256" algorithm identifier so the JJWT library knows what to use.
        String secret_key = secretClientProvider.getSecret(jwtSecretName);
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

    // parses + validates the JWT; expired/bad tokens become UnauthenticatedException
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
