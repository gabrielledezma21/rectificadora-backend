package com.rectificadora.gestion.web;

import com.rectificadora.gestion.domain.Enums;
import com.rectificadora.gestion.repository.RepositorioHistorialTareas;
import com.rectificadora.gestion.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/workshop")
public class HistorialTareasEmpleadoController {

  private static final Set<Enums.WorkTaskAction> ACCIONES_HISTORIAL_EMPLEADO =
      EnumSet.of(Enums.WorkTaskAction.FINALIZADA, Enums.WorkTaskAction.PAUSADA);

  private final RepositorioHistorialTareas repositorioHistorial;
  private final UserRepository repositorioUsuarios;

  HistorialTareasEmpleadoController(RepositorioHistorialTareas repositorioHistorial,
      UserRepository repositorioUsuarios) {
    this.repositorioHistorial = repositorioHistorial;
    this.repositorioUsuarios = repositorioUsuarios;
  }

  record VistaHistorialEmpleado(
      UUID idTarea,
      String descripcionTarea,
      UUID idOrden,
      String numeroOrden,
      ApiDtos.VehicleView vehiculo,
      Enums.WorkTaskAction accion,
      Instant fecha,
      String comentario,
      Enums.WorkTaskStatus estadoActual) {
  }

  @GetMapping("/mi-historial")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public List<VistaHistorialEmpleado> miHistorial(Principal principal) {
    var empleado = repositorioUsuarios.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    if (!empleado.active || empleado.role != Enums.Role.EMPLEADO_TALLER)
      throw new IllegalArgumentException("El usuario no es un empleado de taller activo");

    return repositorioHistorial.buscarHistorialEmpleado(empleado.id, ACCIONES_HISTORIAL_EMPLEADO).stream()
        .map(evento -> {
          var tarea = evento.workOrderItem;
          var orden = tarea.workOrder;
          return new VistaHistorialEmpleado(
              tarea.id,
              tarea.description,
              orden.id,
              orden.orderNumber,
              ApiDtos.vehicle(orden.vehicle),
              evento.action,
              evento.occurredAt,
              evento.comment,
              tarea.taskStatus);
        })
        .toList();
  }
}
