package eu.px.genba.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and parses JWTs for the two token types.
 *
 * <p>HS256 is used for symmetric signing; the secret must be at least 32
 * characters (256 bits). The provider treats secrets shorter than that as a
 * configuration error and refuses to start so production deployments cannot
 * accidentally fall back to the development default.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";
    private static final int MIN_SECRET_BYTES = 32;

    private final String secret;
    @Getter private final long accessTokenExpirationMs;
    @Getter private final long refreshTokenExpirationMs;
    private SecretKey signingKey;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.secret = secret;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @PostConstruct
    void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (got " + bytes.length + ")");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(UUID userId, String email) {
        return generate(userId, email, JwtTokenType.ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return generate(userId, email, JwtTokenType.REFRESH, refreshTokenExpirationMs);
    }

    private String generate(UUID userId, String email, JwtTokenType type, long ttlMs) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMillis(ttlMs));
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse and verify a token. Throws {@link JwtException} on any failure
     * (expired, malformed, bad signature) — caller decides how to respond.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    public JwtTokenType extractType(Claims claims) {
        String raw = claims.get(CLAIM_TYPE, String.class);
        if (raw == null) {
            throw new JwtException("Missing token type claim");
        }
        return JwtTokenType.valueOf(raw);
    }
}
