package com.turing.app.api.application.controller;

import com.turing.app.api.application.dto.AdminApplicationDtos.*;
import com.turing.app.api.application.entity.ApplicationStatus;
import com.turing.app.api.application.service.AdminApplicationService;
import com.turing.app.api.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminApplicationController {
  private final AdminApplicationService service;

  public AdminApplicationController(AdminApplicationService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<Summary> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID periodId,
      @RequestParam(required = false) ApplicationStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return service.list(search, periodId, status, page, size, sort, direction);
  }

  @GetMapping("/{id}")
  public Detail detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  @PostMapping("/{id}/notes")
  @ResponseStatus(HttpStatus.CREATED)
  public Note note(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody NoteRequest body,
      HttpServletRequest request) {
    return service.addNote(user.id(), id, body, request.getRemoteAddr());
  }

  @PatchMapping("/{id}/status")
  public Detail status(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody StatusRequest body,
      HttpServletRequest request) {
    return service.changeStatus(user.id(), id, body, request.getRemoteAddr());
  }
}
