package com.turing.app.api.document.dto;

import com.turing.app.api.document.entity.*;
import java.time.Instant;
import java.util.UUID;

public record StoredFileResponse(
    UUID id,
    UUID applicationId,
    UUID requirementId,
    String requirementName,
    String originalName,
    String mimeType,
    long sizeBytes,
    String checksumSha256,
    FileStatus status,
    Instant uploadedAt,
    long version) {
  public static StoredFileResponse from(StoredFile value) {
    return new StoredFileResponse(
        value.getId(),
        value.getApplication().getId(),
        value.getRequirement().getId(),
        value.getRequirement().getName(),
        value.getOriginalName(),
        value.getMimeType(),
        value.getSizeBytes(),
        value.getChecksumSha256(),
        value.getStatus(),
        value.getUploadedAt(),
        value.getVersion());
  }
}
