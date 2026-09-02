package com.turing.app.api.scholarship.dto;

import com.turing.app.api.scholarship.entity.ScholarshipProgram;
import java.util.UUID;

public record ProgramResponse(
    UUID id, String name, String slug, String description, boolean active, long version) {
  public static ProgramResponse from(ScholarshipProgram v) {
    return new ProgramResponse(
        v.getId(), v.getName(), v.getSlug(), v.getDescription(), v.isActive(), v.getVersion());
  }
}
