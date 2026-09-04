package com.turing.app.api.scholarship.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.scholarship.dto.*;
import com.turing.app.api.scholarship.service.ScholarshipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminScholarshipController {
  private final ScholarshipService service;

  public AdminScholarshipController(ScholarshipService service) {
    this.service = service;
  }

  @GetMapping("/scholarship-programs")
  public List<ProgramResponse> programs(
      @RequestParam(defaultValue = "false") boolean includeArchived) {
    return service.adminPrograms(includeArchived);
  }

  @GetMapping("/scholarship-programs/{id}")
  public ProgramResponse program(@PathVariable UUID id) {
    return service.program(id);
  }

  @PostMapping("/scholarship-programs")
  @ResponseStatus(HttpStatus.CREATED)
  public ProgramResponse createProgram(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @Valid @RequestBody ProgramRequest body,
      HttpServletRequest request) {
    return service.createProgram(actor.id(), body, request.getRemoteAddr());
  }

  @PutMapping("/scholarship-programs/{id}")
  public ProgramResponse updateProgram(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody ProgramRequest body,
      HttpServletRequest request) {
    return service.updateProgram(actor.id(), id, body, request.getRemoteAddr());
  }

  @PostMapping("/scholarship-programs/{id}/archive")
  public ProgramResponse archiveProgram(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.archiveProgram(actor.id(), id, body.version(), request.getRemoteAddr());
  }

  @PostMapping("/scholarship-programs/{id}/restore")
  public ProgramResponse restoreProgram(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.restoreProgram(actor.id(), id, body.version(), request.getRemoteAddr());
  }

  @GetMapping("/application-periods")
  public List<PeriodResponse> periods(@RequestParam UUID programId) {
    return service.adminPeriods(programId);
  }

  @PostMapping("/application-periods")
  @ResponseStatus(HttpStatus.CREATED)
  public PeriodResponse createPeriod(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @Valid @RequestBody PeriodRequest body,
      HttpServletRequest request) {
    return service.createPeriod(actor.id(), body, request.getRemoteAddr());
  }

  @PutMapping("/application-periods/{id}")
  public PeriodResponse updatePeriod(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody PeriodRequest body,
      HttpServletRequest request) {
    return service.updatePeriod(actor.id(), id, body, request.getRemoteAddr());
  }

  @PatchMapping("/application-periods/{id}/status")
  public PeriodResponse status(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody PeriodStatusRequest body,
      HttpServletRequest request) {
    return service.transition(actor.id(), id, body, request.getRemoteAddr());
  }
}
