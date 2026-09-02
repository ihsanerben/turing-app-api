package com.turing.app.api.application.dto;

import com.turing.app.api.application.entity.ApplicationStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class AdminApplicationDtos {
  private AdminApplicationDtos() {}

  public record PageResponse<T>(
      List<T> content, int page, int size, long totalElements, int totalPages) {}

  public record Summary(
      UUID id,
      String studentName,
      String studentEmail,
      UUID periodId,
      String periodName,
      String programName,
      ApplicationStatus status,
      int completion,
      Instant submittedAt,
      Instant createdAt,
      long version) {}

  public record Answer(UUID fieldId, String label, Object value) {}

  public record Document(
      UUID id,
      String requirementName,
      String originalName,
      String mimeType,
      long sizeBytes,
      Instant uploadedAt) {}

  public record Note(UUID id, String adminName, String content, Instant createdAt, long version) {}

  public record History(
      ApplicationStatus oldStatus,
      ApplicationStatus newStatus,
      String changedBy,
      String reason,
      Instant createdAt) {}

  public record Detail(
      Summary application,
      List<Answer> answers,
      List<Document> documents,
      List<Note> notes,
      List<History> history) {}

  public record NoteRequest(@NotBlank @Size(max = 2000) String content) {}

  public record StatusRequest(
      @NotNull ApplicationStatus status,
      @NotNull Long version,
      @NotBlank @Size(max = 500) String reason) {}
}
