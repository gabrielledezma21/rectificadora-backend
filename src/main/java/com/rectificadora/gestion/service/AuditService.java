package com.rectificadora.gestion.service;

import com.rectificadora.gestion.domain.AuditLog;
import com.rectificadora.gestion.repository.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuditService {
  private final AuditLogRepository repo;

  public AuditService(AuditLogRepository repo) {
    this.repo = repo;
  }

  public void record(String action, String type, UUID id, String detail) {
    var a = new AuditLog();
    var auth = SecurityContextHolder.getContext().getAuthentication();
    a.username = auth == null ? "Sistema" : auth.getName();
    a.action = action;
    a.entityType = type;
    a.entityId = id == null ? null : id.toString();
    a.detail = detail;
    repo.save(a);
  }
}
