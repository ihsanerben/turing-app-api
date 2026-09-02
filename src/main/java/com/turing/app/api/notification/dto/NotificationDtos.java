package com.turing.app.api.notification.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class NotificationDtos {
  private NotificationDtos() {}

  public record CampaignRequest(
      @NotBlank @Size(max = 200) String subject,
      @NotBlank @Size(max = 10000) String body,
      @NotEmpty @Size(max = 500) Set<UUID> userIds) {}

  public record CampaignSummary(
      UUID id,
      String subject,
      String status,
      int recipientCount,
      int sentCount,
      int failedCount,
      Instant createdAt,
      long version) {}

  public record Recipient(
      UUID id,
      UUID userId,
      String email,
      String status,
      int attemptCount,
      String failureMessage,
      Instant sentAt) {}

  public record CampaignDetail(
      UUID id,
      String subject,
      String body,
      String status,
      List<Recipient> recipients,
      Instant createdAt,
      long version) {}

  public record VersionRequest(@PositiveOrZero long version) {}

  public record NotificationResponse(
      UUID id,
      String title,
      String message,
      String type,
      String relatedType,
      UUID relatedId,
      Instant readAt,
      Instant createdAt) {}
}
