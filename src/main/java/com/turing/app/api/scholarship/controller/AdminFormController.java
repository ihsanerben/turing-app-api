package com.turing.app.api.scholarship.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.scholarship.dto.*;
import com.turing.app.api.scholarship.service.FormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminFormController {
  private final FormService service;

  public AdminFormController(FormService service) {
    this.service = service;
  }

  @GetMapping("/application-periods/{periodId}/forms")
  public List<FormSummaryResponse> list(@PathVariable UUID periodId) {
    return service.list(periodId);
  }

  @PostMapping("/application-periods/{periodId}/forms")
  @ResponseStatus(HttpStatus.CREATED)
  public FormResponse create(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID periodId,
      @Valid @RequestBody FormCreateRequest body,
      HttpServletRequest request) {
    return service.create(actor.id(), periodId, body, request.getRemoteAddr());
  }

  @GetMapping("/forms/{id}")
  public FormResponse get(@PathVariable UUID id) {
    return service.get(id);
  }

  @PutMapping("/forms/{id}/schema")
  public FormResponse save(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody FormSchemaRequest body,
      HttpServletRequest request) {
    return service.saveSchema(actor.id(), id, body, request.getRemoteAddr());
  }

  @PostMapping("/forms/{id}/publish")
  public FormResponse publish(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.publish(actor.id(), id, body, request.getRemoteAddr());
  }

  @PostMapping("/forms/{id}/new-version")
  @ResponseStatus(HttpStatus.CREATED)
  public FormResponse newVersion(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.newVersion(actor.id(), id, body, request.getRemoteAddr());
  }
}
