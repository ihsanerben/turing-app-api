package com.turing.app.api.content.controller;

import static com.turing.app.api.content.dto.ContentDtos.*;

import com.turing.app.api.content.service.ContentService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicContentController {
  private final ContentService service;

  public PublicContentController(ContentService service) {
    this.service = service;
  }

  @GetMapping("/announcements")
  public List<AnnouncementSummary> announcements() {
    return service.publicAnnouncements();
  }

  @GetMapping("/announcements/{slug}")
  public PublicAnnouncement announcement(@PathVariable String slug) {
    return service.publicAnnouncement(slug);
  }
}
