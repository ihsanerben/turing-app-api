package com.turing.app.api.auth.repository;

import com.turing.app.api.auth.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
}
