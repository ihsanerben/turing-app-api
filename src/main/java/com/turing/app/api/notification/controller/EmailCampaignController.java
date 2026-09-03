package com.turing.app.api.notification.controller;

import static com.turing.app.api.notification.dto.NotificationDtos.*;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.notification.service.EmailCampaignService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/email-campaigns")
public class EmailCampaignController {
  private final EmailCampaignService service;

  public EmailCampaignController(EmailCampaignService service) {
    this.service = service;
  }

  @GetMapping
  public List<CampaignSummary> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public CampaignDetail detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CampaignDetail create(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CampaignRequest body,
      HttpServletRequest request) {
    return service.create(user.id(), body, request.getRemoteAddr());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public CampaignDetail createWithAttachment(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestPart("campaign") CampaignRequest body,
      @RequestPart(value = "attachment", required = false) MultipartFile attachment,
      HttpServletRequest request) {
    return service.create(user.id(), body, attachment, request.getRemoteAddr());
  }

  @PostMapping("/{id}/send")
  public CampaignDetail send(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.send(user.id(), id, body.version(), request.getRemoteAddr());
  }

  @PostMapping("/{id}/retry-failed")
  public CampaignDetail retry(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable UUID id,
      @Valid @RequestBody VersionRequest body,
      HttpServletRequest request) {
    return service.retry(user.id(), id, body.version(), request.getRemoteAddr());
  }
}
