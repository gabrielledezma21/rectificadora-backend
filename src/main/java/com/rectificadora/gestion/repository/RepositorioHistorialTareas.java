package com.rectificadora.gestion.repository;

import com.rectificadora.gestion.domain.Enums;
import com.rectificadora.gestion.domain.WorkTaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RepositorioHistorialTareas extends JpaRepository<WorkTaskHistory, UUID> {

  @Query("""
      select h from WorkTaskHistory h
      join fetch h.workOrderItem i
      join fetch i.workOrder o
      left join fetch o.vehicle
      where h.employeeId = :idEmpleado
        and h.action in :acciones
      order by h.occurredAt desc
      """)
  List<WorkTaskHistory> buscarHistorialEmpleado(
      @Param("idEmpleado") UUID idEmpleado,
      @Param("acciones") Collection<Enums.WorkTaskAction> acciones);
}
