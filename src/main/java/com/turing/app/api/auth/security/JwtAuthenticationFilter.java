package com.turing.app.api.auth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String token = cookie(request, CookieService.ACCESS);
    if (token != null) {
      try {
        Jwt jwt = jwtService.decode(token);
        if ("access".equals(jwt.getClaimAsString("type"))) {
          AuthenticatedUser user =
              new AuthenticatedUser(
                  UUID.fromString(jwt.getSubject()),
                  jwt.getClaimAsString("email"),
                  jwt.getClaimAsString("role"));
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                      user,
                      null,
                      java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))));
        }
      } catch (RuntimeException ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }

  public static String cookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) return null;
    return Arrays.stream(request.getCookies())
        .filter(cookie -> name.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
