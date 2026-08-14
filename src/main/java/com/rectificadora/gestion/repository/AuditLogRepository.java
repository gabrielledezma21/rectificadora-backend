package com.rectificadora.gestion.repository;
import com.rectificadora.gestion.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> { List<AuditLog> findTop200ByOrderByOccurredAtDesc(); }
