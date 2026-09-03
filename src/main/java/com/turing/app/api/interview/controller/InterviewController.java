package com.turing.app.api.interview.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.interview.dto.*;
import com.turing.app.api.interview.service.InterviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class InterviewController {
  private final InterviewService service;

  public InterviewController(InterviewService service) {
    this.service = service;
  }

  @GetMapping("/api/me/interviews")
  public List<StudentInterviewResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
    return service.mine(user.id());
  }

  @GetMapping("/api/me/interviews/{id}")
  public StudentInterviewResponse mine(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
    return service.mine(user.id(), id);
  }

  @GetMapping("/api/admin/applications/{applicationId}/interviews")
  public List<AdminInterviewResponse> application(@PathVariable UUID applicationId) {
    return service.byApplication(applicationId);
  }

  @GetMapping("/api/admin/interviews")
  public List<AdminInterviewResponse> all() {
    return service.all();
  }

  @PostMapping("/api/admin/applications/{applicationId}/interviews")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminInterviewResponse create(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID applicationId,
      @Valid @RequestBody InterviewRequest body,
      HttpServletRequest request) {
    return service.create(user.id(), applicationId, body, request.getRemoteAddr());
  }

  @PostMapping("/api/admin/interviews/bulk")
  @ResponseStatus(HttpStatus.CREATED)
  public List<AdminInterviewResponse> createBulk(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody BulkInterviewRequest body,
      HttpServletRequest request) {
    return service.createBulk(user.id(), body, request.getRemoteAddr());
  }

  @PutMapping("/api/admin/interviews/{id}")
  public AdminInterviewResponse update(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody InterviewRequest body,
      HttpServletRequest request) {
    return service.update(user.id(), id, body, request.getRemoteAddr());
  }

  @PatchMapping("/api/admin/interviews/{id}/status")
  public AdminInterviewResponse status(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody InterviewStatusRequest body,
      HttpServletRequest request) {
    return service.transition(user.id(), id, body, request.getRemoteAddr());
  }

  @PutMapping("/api/admin/interviews/{id}/feedback")
  public AdminInterviewResponse feedback(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody FeedbackRequest body,
      HttpServletRequest request) {
    return service.upsertFeedback(user.id(), id, body, request.getRemoteAddr());
  }
}
