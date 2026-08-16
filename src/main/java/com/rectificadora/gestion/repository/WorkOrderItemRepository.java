package com.rectificadora.gestion.repository;

import com.rectificadora.gestion.domain.Enums;
import com.rectificadora.gestion.domain.WorkOrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from WorkOrderItem i join fetch i.workOrder left join fetch i.assignedEmployee where i.id=:id")
  Optional<WorkOrderItem> findLockedById(@Param("id") UUID id);

  boolean existsByAssignedEmployeeIdAndTaskStatusIn(UUID employeeId, Collection<Enums.WorkTaskStatus> statuses);
}
