package com.turing.app.api.content.dto;

import com.turing.app.api.content.entity.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class ContentDtos {
  private ContentDtos() {}

  public record AnnouncementRequest(
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 200) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
      @NotBlank @Size(max = 500) String summary,
      @NotBlank @Size(max = 30000) String content,
      @PositiveOrZero Long version) {}

  public record AnnouncementResponse(
      UUID id,
      String title,
      String slug,
      String summary,
      String content,
      AnnouncementStatus status,
      Instant publishedAt,
      Instant createdAt,
      long version) {
    public static AnnouncementResponse from(Announcement v) {
      return new AnnouncementResponse(
          v.getId(),
          v.getTitle(),
          v.getSlug(),
          v.getSummary(),
          v.getContent(),
          v.getStatus(),
          v.getPublishedAt(),
          v.getCreatedAt(),
          v.getVersion());
    }
  }

  public record AnnouncementSummary(
      UUID id, String title, String slug, String summary, Instant publishedAt) {}

  public record PublicAnnouncement(
      UUID id, String title, String slug, String summary, String content, Instant publishedAt) {}

  public record VersionRequest(@NotNull @PositiveOrZero Long version) {}
}
