package com.turing.app.api.audit.dto;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record AuditLogResponse(
    UUID id,
    UUID actorId,
    String actorName,
    String actorEmail,
    String action,
    String entityType,
    UUID entityId,
    JsonNode oldValues,
    JsonNode newValues,
    String ipReference,
    String requestId,
    Instant createdAt) {}
