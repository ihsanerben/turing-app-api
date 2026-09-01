package com.turing.app.api.auth.controller;

import com.turing.app.api.auth.dto.*;
import com.turing.app.api.auth.security.*;
import com.turing.app.api.auth.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieService cookies;
    public AuthController(AuthService authService, CookieService cookies) { this.authService = authService; this.cookies = cookies; }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken()); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request); return new MessageResponse("Kayıt oluşturuldu. E-postanızı doğrulayın.");
    }
    @PostMapping("/verify-email")
    public MessageResponse verify(@Valid @RequestBody TokenRequest request) {
        authService.verifyEmail(request.token()); return new MessageResponse("E-posta adresi doğrulandı.");
    }
    @PostMapping("/resend-verification")
    public MessageResponse resend(@Valid @RequestBody EmailRequest request) { return new MessageResponse(authService.resendVerification(request.email())); }
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookies(authService.login(request));
    }
    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(HttpServletRequest request) {
        String raw = JwtAuthenticationFilter.cookie(request, CookieService.REFRESH);
        if (raw == null) throw new com.turing.app.api.auth.exception.AuthException(HttpStatus.UNAUTHORIZED, "MISSING_REFRESH_TOKEN", "Oturum yenilenemedi.");
        return withCookies(authService.refresh(raw));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(JwtAuthenticationFilter.cookie(request, CookieService.REFRESH));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString()).build();
    }
    @PostMapping("/forgot-password")
    public MessageResponse forgot(@Valid @RequestBody EmailRequest request) { return new MessageResponse(authService.forgotPassword(request.email())); }
    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request); return new MessageResponse("Şifreniz yenilendi. Tekrar giriş yapabilirsiniz.");
    }
    private ResponseEntity<UserResponse> withCookies(AuthTokens tokens) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.access(tokens.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(tokens.refreshToken()).toString())
                .body(UserResponse.from(tokens.user()));
    }
}
