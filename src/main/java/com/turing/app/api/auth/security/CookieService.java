package com.turing.app.api.auth.security;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {
  public static final String ACCESS = "TURING_ACCESS_TOKEN";
  public static final String REFRESH = "TURING_REFRESH_TOKEN";
  private final AuthProperties properties;

  public CookieService(AuthProperties properties) {
    this.properties = properties;
  }

  public ResponseCookie access(String token) {
    return cookie(ACCESS, token, "/", properties.accessExpiration());
  }

  public ResponseCookie refresh(String token) {
    return cookie(REFRESH, token, "/api/auth", properties.refreshExpiration());
  }

  public ResponseCookie clearAccess() {
    return cookie(ACCESS, "", "/", Duration.ZERO);
  }

  public ResponseCookie clearRefresh() {
    return cookie(REFRESH, "", "/api/auth", Duration.ZERO);
  }

  private ResponseCookie cookie(String name, String value, String path, Duration age) {
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(properties.cookieSecure())
            .sameSite(properties.cookieSameSite())
            .path(path)
            .maxAge(age);
    if (properties.cookieDomain() != null && !properties.cookieDomain().isBlank())
      builder.domain(properties.cookieDomain());
    return builder.build();
  }
}
