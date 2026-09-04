package com.turing.app.api.auth.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(
    String jwtSecret,
    Duration accessExpiration,
    Duration refreshExpiration,
    Duration verificationExpiration,
    boolean emailVerificationRequired,
    Duration passwordResetExpiration,
    boolean cookieSecure,
    String cookieSameSite,
    String cookieDomain,
    String frontendBaseUrl,
    String mailFrom,
    int rateLimitMaxAttempts,
    Duration rateLimitWindow) {}
