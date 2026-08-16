package com.rectificadora.gestion.web;

import com.rectificadora.gestion.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/backups")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {
  private final ClientRepository clients;
  private final WorkOrderRepository orders;
  private final CatalogTaskRepository tasks;
  private final UserRepository users;
  private final AuditLogRepository audit;

  BackupController(ClientRepository clients, WorkOrderRepository orders, CatalogTaskRepository tasks,
      UserRepository users, AuditLogRepository audit) {
    this.clients = clients;
    this.orders = orders;
    this.tasks = tasks;
    this.users = users;
    this.audit = audit;
  }

  @GetMapping
  @Transactional
  public Map<String, Object> export() {
    var result = new LinkedHashMap<String, Object>();
    result.put("version", 1);
    result.put("exportedAt", Instant.now());
    result.put("clients", clients.findAll().stream().map(ApiDtos::client).toList());
    result.put("orders", orders.findAll().stream().map(ApiDtos::order).toList());
    result.put("tasks", tasks.findAll().stream().map(ApiDtos::task).toList());
    result.put("users", users.findAll().stream().map(ApiDtos::user).toList());
    result.put("audit", audit.findTop200ByOrderByOccurredAtDesc());
    return result;
  }
}
