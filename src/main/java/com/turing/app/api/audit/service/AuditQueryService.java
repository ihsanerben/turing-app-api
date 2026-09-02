package com.turing.app.api.audit.service;

import com.turing.app.api.audit.dto.AuditLogPageResponse;
import com.turing.app.api.audit.dto.AuditLogResponse;
import com.turing.app.api.audit.exception.AuditException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditQueryService {
  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public AuditQueryService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public AuditLogPageResponse list(
      String action,
      String entityType,
      UUID actorId,
      UUID entityId,
      int page,
      int size,
      String direction) {
    validatePage(page, size);
    String order = validateDirection(direction);
    List<String> filters = new ArrayList<>();
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    addTextFilter(filters, parameters, "action", action, "l.action");
    addTextFilter(filters, parameters, "entityType", entityType, "l.entity_type");
    addUuidFilter(filters, parameters, "actorId", actorId, "l.actor_id");
    addUuidFilter(filters, parameters, "entityId", entityId, "l.entity_id");

    String where = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
    long total =
        jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs l" + where, parameters, Long.class);
    parameters.addValue("limit", size).addValue("offset", (long) page * size);
    List<AuditLogResponse> content =
        jdbc.query(
            """
            SELECT l.id,
                   l.actor_id,
                   u.first_name,
                   u.last_name,
                   u.email,
                   l.action,
                   l.entity_type,
                   l.entity_id,
                   l.old_values::text AS old_values,
                   l.new_values::text AS new_values,
                   l.ip_reference,
                   l.request_id,
                   l.created_at
            FROM audit_logs l
            JOIN users u ON u.id = l.actor_id
            """
                + where
                + " ORDER BY l.created_at "
                + order
                + ", l.id "
                + order
                + " LIMIT :limit OFFSET :offset",
            parameters,
            (result, rowNumber) ->
                new AuditLogResponse(
                    result.getObject("id", UUID.class),
                    result.getObject("actor_id", UUID.class),
                    result.getString("first_name") + " " + result.getString("last_name"),
                    result.getString("email"),
                    result.getString("action"),
                    result.getString("entity_type"),
                    result.getObject("entity_id", UUID.class),
                    objectMapper.readTree(result.getString("old_values")),
                    objectMapper.readTree(result.getString("new_values")),
                    result.getString("ip_reference"),
                    result.getString("request_id"),
                    result.getObject("created_at", OffsetDateTime.class).toInstant()));
    int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
    return new AuditLogPageResponse(content, page, size, total, totalPages);
  }

  private void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AuditException(
          HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Sayfalama değerlerini kontrol edin.");
    }
  }

  private String validateDirection(String direction) {
    if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
      throw new AuditException(
          HttpStatus.BAD_REQUEST, "INVALID_SORT_DIRECTION", "Sıralama yönüne izin verilmiyor.");
    }
    return direction.toUpperCase(Locale.ROOT);
  }

  private void addTextFilter(
      List<String> filters,
      MapSqlParameterSource parameters,
      String name,
      String value,
      String column) {
    if (value != null && !value.isBlank()) {
      filters.add(column + " = :" + name);
      parameters.addValue(name, value.trim().toUpperCase(Locale.ROOT));
    }
  }

  private void addUuidFilter(
      List<String> filters,
      MapSqlParameterSource parameters,
      String name,
      UUID value,
      String column) {
    if (value != null) {
      filters.add(column + " = :" + name);
      parameters.addValue(name, value);
    }
  }
}
