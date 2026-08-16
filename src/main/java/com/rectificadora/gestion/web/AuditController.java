package com.rectificadora.gestion.web;

import com.rectificadora.gestion.domain.AuditLog;
import com.rectificadora.gestion.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditLogRepository repo;

    AuditController(AuditLogRepository r) {
        repo = r;
    }

    @GetMapping
    public List<AuditLog> list() {
        return repo.findTop200ByOrderByOccurredAtDesc();
    }
}
