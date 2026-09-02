package com.turing.app.api.auth.service;

import com.turing.app.api.auth.dto.*;
import com.turing.app.api.auth.entity.*;
import com.turing.app.api.auth.exception.AuthException;
import com.turing.app.api.auth.repository.*;
import com.turing.app.api.auth.security.*;
import com.turing.app.api.user.entity.*;
import com.turing.app.api.user.repository.UserRepository;
import java.time.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final String GENERIC_EMAIL_MESSAGE =
      "E-posta adresi uygunsa bir bağlantı gönderildi.";
  private final UserRepository users;
  private final AuthSessionRepository sessions;
  private final EmailVerificationTokenRepository verificationTokens;
  private final PasswordResetTokenRepository resetTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final TokenHashService hashes;
  private final AuthProperties properties;
  private final AuthMailService mail;
  private final Clock clock;

  public AuthService(
      UserRepository users,
      AuthSessionRepository sessions,
      EmailVerificationTokenRepository verificationTokens,
      PasswordResetTokenRepository resetTokens,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      TokenHashService hashes,
      AuthProperties properties,
      AuthMailService mail,
      Clock clock) {
    this.users = users;
    this.sessions = sessions;
    this.verificationTokens = verificationTokens;
    this.resetTokens = resetTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.hashes = hashes;
    this.properties = properties;
    this.mail = mail;
    this.clock = clock;
  }

  @Transactional
  public void register(RegisterRequest request) {
    if (users.existsByEmailIgnoreCase(request.email().trim()))
      throw new AuthException(
          HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Bu e-posta adresi zaten kullanılıyor.");
    Instant now = clock.instant();
    User user =
        users.save(
            User.pending(
                request.email().trim(),
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                now));
    sendVerification(user, now);
  }

  @Transactional
  public void verifyEmail(String rawToken) {
    Instant now = clock.instant();
    EmailVerificationToken token =
        verificationTokens
            .findByTokenHash(hashes.hash(rawToken))
            .orElseThrow(() -> invalidToken("Doğrulama bağlantısı geçersiz."));
    if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now))
      throw invalidToken("Doğrulama bağlantısının süresi dolmuş.");
    token.use(now);
    token.getUser().verify(now);
  }

  @Transactional
  public String resendVerification(String email) {
    users
        .findByEmailIgnoreCase(email.trim())
        .filter(user -> user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION)
        .ifPresent(user -> sendVerification(user, clock.instant()));
    return GENERIC_EMAIL_MESSAGE;
  }

  @Transactional
  public AuthTokens login(LoginRequest request) {
    User user =
        users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(this::badCredentials);
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
      throw badCredentials();
    if (user.getAccountStatus() != AccountStatus.ACTIVE)
      throw new AuthException(
          HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Hesap henüz aktif değil.");
    Instant now = clock.instant();
    user.login(now);
    return createSession(user, UUID.randomUUID(), now);
  }

  @Transactional(noRollbackFor = AuthException.class)
  public AuthTokens refresh(String rawToken) {
    Jwt jwt = decodeRefresh(rawToken);
    Instant now = clock.instant();
    AuthSession old =
        sessions
            .findByTokenHash(hashes.hash(rawToken))
            .orElseThrow(() -> unauthorized("INVALID_REFRESH_TOKEN", "Oturum yenilenemedi."));
    if (old.getRevokedAt() != null) {
      sessions.revokeFamily(old.getFamilyId(), now);
      throw unauthorized("REFRESH_TOKEN_REUSED", "Oturum güvenlik nedeniyle sonlandırıldı.");
    }
    if (!old.getExpiresAt().isAfter(now)
        || !old.getId().toString().equals(jwt.getClaimAsString("sid"))) {
      old.revoke(now);
      throw unauthorized("INVALID_REFRESH_TOKEN", "Oturum yenilenemedi.");
    }
    User user = old.getUser();
    if (user.getAccountStatus() != AccountStatus.ACTIVE)
      throw unauthorized("ACCOUNT_NOT_ACTIVE", "Hesap aktif değil.");
    return rotate(old, user, now);
  }

  private AuthTokens rotate(AuthSession old, User user, Instant now) {
    UUID sessionId = UUID.randomUUID();
    String refresh = jwtService.refreshToken(user, sessionId, now);
    AuthSession next =
        AuthSession.createWithId(
            sessionId,
            user,
            old.getFamilyId(),
            hashes.hash(refresh),
            now.plus(properties.refreshExpiration()),
            now);
    sessions.save(next);
    old.rotateTo(next.getId(), now);
    return new AuthTokens(jwtService.accessToken(user, now), refresh, user);
  }

  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    sessions
        .findByTokenHash(hashes.hash(rawToken))
        .ifPresent(session -> session.revoke(clock.instant()));
  }

  @Transactional
  public String forgotPassword(String email) {
    users
        .findByEmailIgnoreCase(email.trim())
        .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
        .ifPresent(
            user -> {
              Instant now = clock.instant();
              String raw = hashes.randomToken();
              resetTokens.save(
                  PasswordResetToken.create(
                      user, hashes.hash(raw), now.plus(properties.passwordResetExpiration()), now));
              mail.sendPasswordReset(user.getEmail(), raw);
            });
    return GENERIC_EMAIL_MESSAGE;
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    Instant now = clock.instant();
    PasswordResetToken token =
        resetTokens
            .findByTokenHash(hashes.hash(request.token()))
            .orElseThrow(() -> invalidToken("Şifre yenileme bağlantısı geçersiz."));
    if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now))
      throw invalidToken("Şifre yenileme bağlantısının süresi dolmuş.");
    token.use(now);
    token.getUser().changePassword(passwordEncoder.encode(request.password()), now);
    sessions.revokeAllForUser(token.getUser().getId(), now);
  }

  @Transactional(readOnly = true)
  public UserResponse currentUser(UUID id) {
    return UserResponse.from(users.findById(id).orElseThrow(this::badCredentials));
  }

  private AuthTokens createSession(User user, UUID familyId, Instant now) {
    UUID sessionId = UUID.randomUUID();
    String refresh = jwtService.refreshToken(user, sessionId, now);
    sessions.save(
        AuthSession.createWithId(
            sessionId,
            user,
            familyId,
            hashes.hash(refresh),
            now.plus(properties.refreshExpiration()),
            now));
    return new AuthTokens(jwtService.accessToken(user, now), refresh, user);
  }

  private void sendVerification(User user, Instant now) {
    String raw = hashes.randomToken();
    verificationTokens.save(
        EmailVerificationToken.create(
            user, hashes.hash(raw), now.plus(properties.verificationExpiration()), now));
    mail.sendVerification(user.getEmail(), raw);
  }

  private Jwt decodeRefresh(String raw) {
    try {
      Jwt jwt = jwtService.decode(raw);
      if (!"refresh".equals(jwt.getClaimAsString("type")))
        throw new JwtException("Wrong token type");
      return jwt;
    } catch (JwtException | IllegalArgumentException exception) {
      throw unauthorized("INVALID_REFRESH_TOKEN", "Oturum yenilenemedi.");
    }
  }

  private AuthException badCredentials() {
    return unauthorized("BAD_CREDENTIALS", "E-posta veya şifre hatalı.");
  }

  private AuthException invalidToken(String message) {
    return new AuthException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", message);
  }

  private AuthException unauthorized(String code, String message) {
    return new AuthException(HttpStatus.UNAUTHORIZED, code, message);
  }
}
