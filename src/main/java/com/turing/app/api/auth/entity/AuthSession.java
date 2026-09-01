package com.turing.app.api.auth.entity;

import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "family_id", nullable = false) private UUID familyId;
    @Column(name = "token_hash", nullable = false, length = 64, unique = true) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "replaced_by_session_id") private UUID replacedBySessionId;
    @Column(name = "last_used_at") private Instant lastUsedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AuthSession() {}
    public static AuthSession create(User user, UUID familyId, String hash, Instant expiresAt, Instant now) {
        return createWithId(UUID.randomUUID(), user, familyId, hash, expiresAt, now);
    }
    public static AuthSession createWithId(UUID id, User user, UUID familyId, String hash, Instant expiresAt, Instant now) {
        AuthSession value = new AuthSession();
        value.id = id; value.user = user; value.familyId = familyId;
        value.tokenHash = hash; value.expiresAt = expiresAt; value.createdAt = now;
        return value;
    }
    public void rotateTo(UUID replacement, Instant now) { revokedAt = now; replacedBySessionId = replacement; lastUsedAt = now; }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public UUID getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
