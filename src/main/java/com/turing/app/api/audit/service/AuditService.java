package com.turing.app.api.audit.service;

import java.time.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;
    public AuditService(NamedParameterJdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    public void recordProfileCorrection(UUID actorId, UUID profileId, String oldValues, String newValues, String ip) {
        record(actorId, "PROFILE_CORRECTED", "STUDENT_PROFILE", profileId, oldValues, newValues, ip);
    }
    public void record(UUID actorId, String action, String entityType, UUID entityId, String oldValues, String newValues, String ip) {
        jdbc.update("""
                INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, old_values, new_values, ip_reference, request_id, created_at)
                VALUES (:id, :actor, :action, :entityType, :entity, CAST(:old AS jsonb), CAST(:new AS jsonb), :ip, :requestId, :createdAt)
                """, Map.of("id", UUID.randomUUID(), "actor", actorId, "action", action, "entityType", entityType,
                        "entity", entityId, "old", oldValues,
                        "new", newValues, "ip", ip == null ? "" : ip, "requestId", Objects.toString(MDC.get("requestId"), ""),
                        "createdAt", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)));
    }
}
