package com.turing.app.api.profile.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.profile.dto.*;
import com.turing.app.api.profile.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProfileController {
  private final ProfileService profiles;

  public ProfileController(ProfileService profiles) {
    this.profiles = profiles;
  }

  @GetMapping("/api/me/profile")
  public ProfileResponse own(@AuthenticationPrincipal AuthenticatedUser user) {
    return profiles.get(user.id());
  }

  @PutMapping("/api/me/profile")
  public ProfileResponse updateOwn(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody ProfileUpdateRequest request) {
    return profiles.updateOwn(user.id(), request);
  }

  @GetMapping("/api/universities")
  public List<UniversityResponse> universities() {
    return profiles.universities();
  }

  @GetMapping("/api/universities/{id}/departments")
  public List<DepartmentResponse> departments(@PathVariable UUID id) {
    return profiles.departments(id);
  }

  @GetMapping("/api/admin/users/{userId}/profile")
  public ProfileResponse adminGet(@PathVariable UUID userId) {
    return profiles.get(userId);
  }

  @PutMapping("/api/admin/users/{userId}/profile")
  public ProfileResponse adminUpdate(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID userId,
      @Valid @RequestBody ProfileUpdateRequest request,
      HttpServletRequest servletRequest) {
    return profiles.updateByAdmin(actor.id(), userId, request, servletRequest.getRemoteAddr());
  }
}
