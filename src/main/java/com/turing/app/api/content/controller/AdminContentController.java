package com.turing.app.api.content.controller;

import static com.turing.app.api.content.dto.ContentDtos.*;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.content.service.ContentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminContentController {
  private final ContentService service;

  public AdminContentController(ContentService service) {
    this.service = service;
  }

  @GetMapping("/announcements")
  public List<AnnouncementResponse> announcements() {
    return service.adminAnnouncements();
  }

  @GetMapping("/announcements/{id}")
  public AnnouncementResponse announcement(@PathVariable UUID id) {
    return service.adminAnnouncement(id);
  }

  @PostMapping("/announcements")
  @ResponseStatus(HttpStatus.CREATED)
  public AnnouncementResponse createAnnouncement(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody AnnouncementRequest body,
      HttpServletRequest request) {
    return service.createAnnouncement(user.id(), body, request.getRemoteAddr());
  }

  @PutMapping("/announcements/{id}")
  public AnnouncementResponse updateAnnouncement(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody AnnouncementRequest body,
      HttpServletRequest request) {
    return service.updateAnnouncement(user.id(), id, body, request.getRemoteAddr());
  }

  @PostMapping("/announcements/{id}/publish")
  public AnnouncementResponse publish(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.publish(user.id(), id, body.version(), request.getRemoteAddr());
  }

  @PostMapping("/announcements/{id}/archive")
  public AnnouncementResponse archiveAnnouncement(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.archiveAnnouncement(user.id(), id, body.version(), request.getRemoteAddr());
  }

  @PostMapping("/announcements/{id}/restore")
  public AnnouncementResponse restoreAnnouncement(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.restoreAnnouncement(user.id(), id, body.version(), request.getRemoteAddr());
  }

  @DeleteMapping("/announcements/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAnnouncement(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @RequestParam long version,
      HttpServletRequest request) {
    service.deleteAnnouncement(user.id(), id, version, request.getRemoteAddr());
  }
}
