package com.turing.app.api.evaluation.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.evaluation.dto.*;
import com.turing.app.api.evaluation.dto.EvaluationDtos.*;
import com.turing.app.api.evaluation.service.EvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminEvaluationController {
  private final EvaluationService service;

  public AdminEvaluationController(EvaluationService service) {
    this.service = service;
  }

  @GetMapping("/application-periods/{periodId}/evaluation-criteria")
  public List<CriterionResponse> criteria(@PathVariable UUID periodId) {
    return service.criteria(periodId);
  }

  @PostMapping("/application-periods/{periodId}/evaluation-criteria")
  @ResponseStatus(HttpStatus.CREATED)
  public CriterionResponse create(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID periodId,
      @Valid @RequestBody CriterionRequest body,
      HttpServletRequest request) {
    return service.create(actor.id(), periodId, body, request.getRemoteAddr());
  }

  @PutMapping("/evaluation-criteria/{id}")
  public CriterionResponse update(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @Valid @RequestBody CriterionRequest body,
      HttpServletRequest request) {
    return service.update(actor.id(), id, body, request.getRemoteAddr());
  }

  @DeleteMapping("/evaluation-criteria/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID id,
      @RequestParam Long version,
      HttpServletRequest request) {
    service.delete(actor.id(), id, version, request.getRemoteAddr());
  }

  @PutMapping("/applications/{applicationId}/evaluation-scores/{criterionId}")
  public ApplicationEvaluation score(
      @AuthenticationPrincipal AuthenticatedUser actor,
      @PathVariable UUID applicationId,
      @PathVariable UUID criterionId,
      @Valid @RequestBody ScoreRequest body,
      HttpServletRequest request) {
    return service.upsertScore(
        actor.id(), applicationId, criterionId, body, request.getRemoteAddr());
  }

  @GetMapping("/applications/{applicationId}/evaluation")
  public ApplicationEvaluation evaluation(@PathVariable UUID applicationId) {
    return service.evaluation(applicationId);
  }

  @GetMapping("/application-periods/{periodId}/ranking")
  public List<Ranking> ranking(@PathVariable UUID periodId) {
    return service.ranking(periodId);
  }
}
