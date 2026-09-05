package com.turing.app.api.participation.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.participation.dto.*;
import com.turing.app.api.participation.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ParticipationController {
  private final ParticipationService service;

  public ParticipationController(ParticipationService service) {
    this.service = service;
  }

  @Operation(summary = "Yemek haftalarını sayfalı listeler")
  @GetMapping({"/api/me/meal-weeks", "/api/admin/meal-weeks"})
  public PageResponse<MealWeekSummary> weeks(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.weeks(page, size);
  }

  @Operation(summary = "Yemek haftasını ve yalnız oturum sahibinin seçimlerini getirir")
  @GetMapping({"/api/me/meal-weeks/{id}", "/api/admin/meal-weeks/{id}"})
  public MealWeekResponse week(
      @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
    return service.week(id, user.id());
  }

  @Operation(summary = "Günleriyle bir yemek haftası açar")
  @PostMapping("/api/admin/meal-weeks")
  @ResponseStatus(HttpStatus.CREATED)
  public MealWeekResponse createWeek(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody MealWeekRequest body,
      HttpServletRequest request) {
    return service.createWeek(user.id(), body, request.getRemoteAddr());
  }

  @Operation(
      summary =
          "Haftalık yemek seçimlerini atomik kaydeder; değişiklik varsa commit sonrasında e-posta özeti gönderir")
  @PutMapping("/api/me/meal-weeks/{id}/selection")
  public SelectionResult meals(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody MealSelectionRequest body) {
    return service.saveMeals(user.id(), id, body);
  }

  @Operation(summary = "Etkinlikleri ve yalnız oturum sahibinin katılımını sayfalı listeler")
  @GetMapping({"/api/me/events", "/api/admin/events"})
  public EventsResponse events(
      @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.events(user.id(), page, size);
  }

  @Operation(summary = "Öğrencilerin katılabileceği bir etkinlik oluşturur")
  @PostMapping("/api/admin/events")
  @ResponseStatus(HttpStatus.CREATED)
  public ActivityResponse createEvent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody EventRequest body,
      HttpServletRequest request) {
    return service.createEvent(user.id(), body, request.getRemoteAddr());
  }

  @Operation(
      summary =
          "Gönderilen etkinlik değişikliklerini atomik kaydeder; diğer sayfalardaki kayıtları korur ve e-posta özeti gönderir")
  @PutMapping("/api/me/events/selection")
  public SelectionResult events(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody EventSelectionRequest body) {
    return service.saveEvents(user.id(), body);
  }

  @Operation(
      summary = "Seçilen yemek günü veya etkinliğin katılımcı adlarını yalnız yöneticiye listeler")
  @GetMapping("/api/admin/participation/{id}/participants")
  public PageResponse<ParticipantResponse> participants(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return service.participants(id, page, size);
  }

  @Operation(summary = "Yemek haftasının günlerini mevcut katılımları koruyarak günceller")
  @PutMapping("/api/admin/meal-weeks/{id}")
  public MealWeekResponse updateWeek(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody MealWeekUpdateRequest body,
      HttpServletRequest request) {
    return service.updateWeek(user.id(), id, body, request.getRemoteAddr());
  }

  @Operation(summary = "Etkinlik bilgilerini günceller ve katılımcılara e-posta gönderir")
  @PutMapping("/api/admin/events/{id}")
  public ActivityResponse updateEvent(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody EventUpdateRequest body,
      HttpServletRequest request) {
    return service.updateEvent(user.id(), id, body, request.getRemoteAddr());
  }
}
