package com.turing.app.api.config.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.config.dto.AdminAppConfigResponse;
import com.turing.app.api.config.dto.AppConfigUpdateRequest;
import com.turing.app.api.config.dto.PublicAppConfigResponse;
import com.turing.app.api.config.service.AppConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AppConfigController {
  private final AppConfigService service;

  public AppConfigController(AppConfigService service) {
    this.service = service;
  }

  @GetMapping("/api/public/app-config")
  public PublicAppConfigResponse publicConfig() {
    return service.publicConfig();
  }

  @GetMapping("/api/admin/app-config")
  public AdminAppConfigResponse adminConfig() {
    return service.adminConfig();
  }

  @PutMapping("/api/admin/app-config")
  public AdminAppConfigResponse update(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @Valid @RequestBody AppConfigUpdateRequest request,
      HttpServletRequest servletRequest) {
    return service.update(actor.id(), request, servletRequest.getRemoteAddr());
  }
}
