package com.turing.app.api.audit.controller;

import com.turing.app.api.audit.dto.AuditLogPageResponse;
import com.turing.app.api.audit.service.AuditQueryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditController {
  private final AuditQueryService service;

  public AdminAuditController(AuditQueryService service) {
    this.service = service;
  }

  @GetMapping
  public AuditLogPageResponse list(
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) UUID actorId,
      @RequestParam(required = false) UUID entityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "desc") String direction) {
    return service.list(action, entityType, actorId, entityId, page, size, direction);
  }
}
