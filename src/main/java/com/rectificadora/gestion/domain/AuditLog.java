package com.rectificadora.gestion.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="audit_logs")
public class AuditLog {
  @Id public UUID id = UUID.randomUUID();
  @Column(nullable=false) public Instant occurredAt = Instant.now();
  @Column(nullable=false) public String username;
  @Column(nullable=false) public String action;
  @Column(nullable=false) public String entityType;
  public String entityId;
  @Column(columnDefinition="text") public String detail;
  public AuditLog() {}
}
