package com.turing.app.api.user.controller;

import com.turing.app.api.auth.dto.UserResponse;
import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.auth.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CurrentUserController {
    private final AuthService authService;
    public CurrentUserController(AuthService authService) { this.authService = authService; }
    @GetMapping("/api/me") public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.currentUser(user.id());
    }
    @GetMapping("/api/admin/ping") public MessageResponse adminPing() { return new MessageResponse("pong"); }
    public record MessageResponse(String message) {}
}
