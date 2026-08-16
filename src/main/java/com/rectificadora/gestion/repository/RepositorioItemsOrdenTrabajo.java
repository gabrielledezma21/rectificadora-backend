package com.rectificadora.gestion.repository;

import com.rectificadora.gestion.domain.Enums;
import com.rectificadora.gestion.domain.ItemOrdenTrabajo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioItemsOrdenTrabajo extends JpaRepository<ItemOrdenTrabajo, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from ItemOrdenTrabajo i join fetch i.ordenTrabajo left join fetch i.empleadoAsignado where i.id=:id")
  Optional<ItemOrdenTrabajo> buscarBloqueadoPorId(@Param("id") UUID id);

  boolean existsByEmpleadoAsignadoIdAndEstadoTareaIn(UUID idEmpleado,
      Collection<Enums.EstadoTareaTaller> estados);
}
