package com.turing.app.api.audience.controller;

import static com.turing.app.api.audience.dto.AudienceListDtos.*;

import com.turing.app.api.audience.service.AudienceListService;
import com.turing.app.api.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/lists")
public class AdminAudienceListController {
  private final AudienceListService service;

  public AdminAudienceListController(AudienceListService service) {
    this.service = service;
  }

  @GetMapping
  public List<Response> all() {
    return service.all();
  }

  @GetMapping("/{id}")
  public Response get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Response create(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreateRequest body,
      HttpServletRequest request) {
    return service.create(user.id(), body, request.getRemoteAddr());
  }

  @PutMapping("/{id}")
  public Response update(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateRequest body,
      HttpServletRequest request) {
    return service.update(user.id(), id, body, request.getRemoteAddr());
  }
}
