package com.turing.app.api.auth.security;

import com.turing.app.api.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final AuthProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public String accessToken(User user, Instant now) {
        return encode(user.getId(), now, now.plus(properties.accessExpiration()), Map.of(
                "type", "access", "email", user.getEmail(), "role", user.getRole().name()));
    }

    public String refreshToken(User user, UUID sessionId, Instant now) {
        return encode(user.getId(), now, now.plus(properties.refreshExpiration()), Map.of(
                "type", "refresh", "sid", sessionId.toString()));
    }

    private String encode(UUID subject, Instant issuedAt, Instant expiresAt, Map<String, Object> claims) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder().subject(subject.toString()).issuedAt(issuedAt)
                .expiresAt(expiresAt).id(UUID.randomUUID().toString());
        claims.forEach(builder::claim);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), builder.build()))
                .getTokenValue();
    }

    public Jwt decode(String value) { return decoder.decode(value); }
}
