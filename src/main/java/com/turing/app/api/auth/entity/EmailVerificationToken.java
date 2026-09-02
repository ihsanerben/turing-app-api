package com.turing.app.api.auth.entity;

import com.turing.app.api.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "token_hash", nullable = false, length = 64, unique = true)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EmailVerificationToken() {}

  public static EmailVerificationToken create(
      User user, String hash, Instant expires, Instant now) {
    EmailVerificationToken token = new EmailVerificationToken();
    token.id = UUID.randomUUID();
    token.user = user;
    token.tokenHash = hash;
    token.expiresAt = expires;
    token.createdAt = now;
    return token;
  }

  public void use(Instant now) {
    usedAt = now;
  }

  public User getUser() {
    return user;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }
}
