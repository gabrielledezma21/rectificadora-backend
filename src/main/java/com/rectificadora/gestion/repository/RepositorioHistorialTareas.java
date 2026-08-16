package com.rectificadora.gestion.repository;

import com.rectificadora.gestion.domain.Enums;
import com.rectificadora.gestion.domain.HistorialTareaTaller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RepositorioHistorialTareas extends JpaRepository<HistorialTareaTaller, UUID> {

  @Query("""
      select h from HistorialTareaTaller h
      join fetch h.itemOrdenTrabajo i
      join fetch i.ordenTrabajo o
      left join fetch o.vehicle
      where h.idEmpleado = :idEmpleado
        and h.accion in :acciones
      order by h.fecha desc
      """)
  List<HistorialTareaTaller> buscarHistorialEmpleado(
      @Param("idEmpleado") UUID idEmpleado,
      @Param("acciones") Collection<Enums.AccionTareaTaller> acciones);
}
