package com.turing.app.api.audience.dto;

import com.turing.app.api.application.entity.ApplicationStatus;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class AudienceListDtos {
  private AudienceListDtos() {}

  public record CreateRequest(
      @NotBlank @Size(max = 200) String name,
      @NotNull UUID programId,
      @NotEmpty Set<UUID> applicationIds) {}

  public record UpdateRequest(
      @NotBlank @Size(max = 200) String name,
      @NotEmpty Set<UUID> applicationIds,
      @NotNull @PositiveOrZero Long version) {}

  public record Member(
      UUID applicationId,
      UUID userId,
      String studentName,
      String email,
      String university,
      String department,
      ApplicationStatus applicationStatus) {}

  public record Response(
      UUID id,
      String name,
      UUID programId,
      String programName,
      List<Member> members,
      Instant createdAt,
      long version) {}
}
