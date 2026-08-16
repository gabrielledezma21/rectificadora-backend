package com.rectificadora.gestion.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.*;
import com.rectificadora.gestion.service.AuditService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/workshop")
public class TareasTallerController {
  private static final Set<Enums.EstadoTareaTaller> ESTADOS_ACTIVOS =
      EnumSet.of(Enums.EstadoTareaTaller.ACEPTADA, Enums.EstadoTareaTaller.EN_PROCESO);

  private final WorkOrderRepository repositorioOrdenes;
  private final RepositorioItemsOrdenTrabajo repositorioItems;
  private final UserRepository repositorioUsuarios;
  private final AuditService auditoria;

  TareasTallerController(WorkOrderRepository repositorioOrdenes, RepositorioItemsOrdenTrabajo repositorioItems,
      UserRepository repositorioUsuarios, AuditService auditoria) {
    this.repositorioOrdenes = repositorioOrdenes;
    this.repositorioItems = repositorioItems;
    this.repositorioUsuarios = repositorioUsuarios;
    this.auditoria = auditoria;
  }

  record VistaEmpleado(UUID id, @JsonProperty("name") String nombre) {
  }

  record VistaHistorial(@JsonProperty("occurredAt") Instant fecha, @JsonProperty("action") Enums.AccionTareaTaller accion,
      String actor, @JsonProperty("employeeId") UUID idEmpleado, @JsonProperty("employeeName") String nombreEmpleado,
      @JsonProperty("comment") String comentario) {
  }

  record VistaTarea(UUID id, @JsonProperty("description") String descripcion,
      @JsonProperty("category") Enums.TaskCategory categoria, @JsonProperty("status") Enums.EstadoTareaTaller estado,
      @JsonProperty("assignedEmployee") VistaEmpleado empleadoAsignado,
      @JsonProperty("technicalNotes") String notasTecnicas, @JsonProperty("history") List<VistaHistorial> historial) {
  }

  record VistaOrden(UUID id, @JsonProperty("orderNumber") String numeroOrden,
      @JsonProperty("status") Enums.OrderStatus estado, @JsonProperty("vehicle") ApiDtos.VehicleView vehiculo,
      @JsonProperty("cylinders") Integer cilindros, @JsonProperty("finalMeasure") String medidaFinal,
      @JsonProperty("receptionDescription") String descripcionRecepcion,
      @JsonProperty("tasks") List<VistaTarea> tareas) {
  }

  record EntradaComentario(@JsonProperty("comment") String comentario) {
  }

  record EntradaPendiente(@JsonProperty("reason") @NotBlank String motivo) {
  }

  record EntradaAsignacion(@JsonProperty("employeeId") UUID idEmpleado,
      @JsonProperty("comment") String comentario) {
  }

  @GetMapping("/orders")
  @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLEADO_TALLER') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @Transactional
  public List<VistaOrden> listar() {
    return repositorioOrdenes.findAll().stream()
        .filter(this::esVisibleEnTaller)
        .sorted(Comparator.comparing((WorkOrder orden) -> orden.createdAt).reversed())
        .map(this::crearVista).toList();
  }

  @PatchMapping("/tasks/{id}/accept")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public VistaOrden aceptar(@PathVariable UUID id, java.security.Principal principal) {
    var empleado = empleadoActual(principal);
    if (repositorioItems.existsByEmpleadoAsignadoIdAndEstadoTareaIn(empleado.id, ESTADOS_ACTIVOS))
      throw new IllegalArgumentException("Ya tenés una tarea activa. Finalizala o dejala pendiente antes de aceptar otra");
    var tarea = tareaBloqueada(id);
    if (!esVisibleEnTaller(tarea.ordenTrabajo))
      throw new IllegalArgumentException("La orden ya no está disponible para el taller");
    if (tarea.estadoTarea == Enums.EstadoTareaTaller.ASIGNADA
        && (tarea.empleadoAsignado == null || !tarea.empleadoAsignado.id.equals(empleado.id)))
      throw new IllegalArgumentException("La tarea está reservada para otro empleado");
    if (tarea.estadoTarea != Enums.EstadoTareaTaller.DISPONIBLE
        && tarea.estadoTarea != Enums.EstadoTareaTaller.PENDIENTE
        && tarea.estadoTarea != Enums.EstadoTareaTaller.ASIGNADA)
      throw new IllegalArgumentException("La tarea ya no está disponible");
    var accion = tarea.estadoTarea == Enums.EstadoTareaTaller.PENDIENTE
        ? Enums.AccionTareaTaller.RETOMADA : Enums.AccionTareaTaller.ACEPTADA;
    tarea.empleadoAsignado = empleado;
    tarea.estadoTarea = Enums.EstadoTareaTaller.ACEPTADA;
    tarea.fechaAceptacion = Instant.now();
    agregarHistorial(tarea, accion, empleado, empleado, null);
    repositorioItems.save(tarea);
    auditoria.record("ACCEPT_TASK", "ORDER_TASK", tarea.id, tarea.descripcion + " · " + empleado.name);
    return crearVista(tarea.ordenTrabajo);
  }

  @PatchMapping("/tasks/{id}/start")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public VistaOrden iniciar(@PathVariable UUID id, java.security.Principal principal) {
    var empleado = empleadoActual(principal);
    var tarea = tareaBloqueada(id);
    verificarResponsable(tarea, empleado);
    if (tarea.estadoTarea != Enums.EstadoTareaTaller.ACEPTADA)
      throw new IllegalArgumentException("Primero debés aceptar la tarea");
    tarea.estadoTarea = Enums.EstadoTareaTaller.EN_PROCESO;
    tarea.fechaInicio = Instant.now();
    if (tarea.ordenTrabajo.status == Enums.OrderStatus.RECEPCION)
      tarea.ordenTrabajo.status = Enums.OrderStatus.EN_PROCESO;
    agregarHistorial(tarea, Enums.AccionTareaTaller.INICIADA, empleado, empleado, null);
    repositorioItems.save(tarea);
    auditoria.record("START_TASK", "ORDER_TASK", tarea.id, tarea.descripcion + " · " + empleado.name);
    return crearVista(tarea.ordenTrabajo);
  }

  @PatchMapping("/tasks/{id}/pending")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public VistaOrden dejarPendiente(@PathVariable UUID id, @Valid @RequestBody EntradaPendiente entrada,
      java.security.Principal principal) {
    var empleado = empleadoActual(principal);
    var tarea = tareaBloqueada(id);
    verificarResponsable(tarea, empleado);
    if (!ESTADOS_ACTIVOS.contains(tarea.estadoTarea))
      throw new IllegalArgumentException("La tarea no está activa");
    agregarNotaTecnica(tarea, empleado.name, entrada.motivo());
    agregarHistorial(tarea, Enums.AccionTareaTaller.PAUSADA, empleado, empleado, entrada.motivo());
    tarea.estadoTarea = Enums.EstadoTareaTaller.PENDIENTE;
    tarea.empleadoAsignado = null;
    repositorioItems.save(tarea);
    auditoria.record("PENDING_TASK", "ORDER_TASK", tarea.id, tarea.descripcion + " · " + entrada.motivo());
    return crearVista(tarea.ordenTrabajo);
  }

  @PatchMapping("/tasks/{id}/complete")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public VistaOrden finalizar(@PathVariable UUID id, @RequestBody(required = false) EntradaComentario entrada,
      java.security.Principal principal) {
    var empleado = empleadoActual(principal);
    var tarea = tareaBloqueada(id);
    verificarResponsable(tarea, empleado);
    if (tarea.estadoTarea != Enums.EstadoTareaTaller.EN_PROCESO)
      throw new IllegalArgumentException("La tarea debe estar en proceso para poder finalizarla");
    String comentario = entrada == null ? null : entrada.comentario();
    agregarNotaTecnica(tarea, empleado.name, comentario);
    tarea.estadoTarea = Enums.EstadoTareaTaller.FINALIZADA;
    tarea.fechaFinalizacion = Instant.now();
    agregarHistorial(tarea, Enums.AccionTareaTaller.FINALIZADA, empleado, empleado, comentario);
    repositorioItems.save(tarea);
    auditoria.record("COMPLETE_TASK", "ORDER_TASK", tarea.id, tarea.descripcion + " · " + empleado.name);
    return crearVista(tarea.ordenTrabajo);
  }

  @PatchMapping("/tasks/{id}/assignment")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public VistaOrden asignar(@PathVariable UUID id, @RequestBody EntradaAsignacion entrada,
      java.security.Principal principal) {
    var actor = repositorioUsuarios.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    var tarea = tareaBloqueada(id);
    if (tarea.estadoTarea == Enums.EstadoTareaTaller.FINALIZADA)
      throw new IllegalArgumentException("Reabrí la tarea antes de cambiar su responsable");
    var anterior = tarea.empleadoAsignado;
    if (entrada.idEmpleado() == null) {
      tarea.empleadoAsignado = null;
      tarea.estadoTarea = Enums.EstadoTareaTaller.DISPONIBLE;
      agregarHistorial(tarea, Enums.AccionTareaTaller.LIBERADA, actor, anterior, entrada.comentario());
    } else {
      var empleado = repositorioUsuarios.findById(entrada.idEmpleado()).orElseThrow();
      if (!empleado.active || empleado.role != Enums.Role.EMPLEADO_TALLER)
        throw new IllegalArgumentException("El responsable debe ser un empleado de taller activo");
      tarea.empleadoAsignado = empleado;
      tarea.estadoTarea = Enums.EstadoTareaTaller.ASIGNADA;
      tarea.fechaAceptacion = null;
      tarea.fechaInicio = null;
      agregarHistorial(tarea, anterior == null ? Enums.AccionTareaTaller.ASIGNADA : Enums.AccionTareaTaller.REASIGNADA,
          actor, empleado, entrada.comentario());
    }
    repositorioItems.save(tarea);
    auditoria.record("ASSIGN_TASK", "ORDER_TASK", tarea.id,
        tarea.descripcion + " · " + (tarea.empleadoAsignado == null ? "sin responsable" : tarea.empleadoAsignado.name));
    return crearVista(tarea.ordenTrabajo);
  }

  @PatchMapping("/tasks/{id}/reopen")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public VistaOrden reabrir(@PathVariable UUID id, @RequestBody(required = false) EntradaComentario entrada,
      java.security.Principal principal) {
    var actor = repositorioUsuarios.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    var tarea = tareaBloqueada(id);
    if (tarea.estadoTarea != Enums.EstadoTareaTaller.FINALIZADA)
      throw new IllegalArgumentException("Solo se pueden reabrir tareas finalizadas");
    tarea.estadoTarea = Enums.EstadoTareaTaller.DISPONIBLE;
    tarea.empleadoAsignado = null;
    tarea.fechaFinalizacion = null;
    agregarHistorial(tarea, Enums.AccionTareaTaller.REABIERTA, actor, null,
        entrada == null ? null : entrada.comentario());
    repositorioItems.save(tarea);
    auditoria.record("REOPEN_TASK", "ORDER_TASK", tarea.id, tarea.descripcion);
    return crearVista(tarea.ordenTrabajo);
  }

  private ItemOrdenTrabajo tareaBloqueada(UUID id) {
    return repositorioItems.buscarBloqueadoPorId(id).orElseThrow();
  }

  private User empleadoActual(java.security.Principal principal) {
    var usuario = repositorioUsuarios.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    if (!usuario.active || usuario.role != Enums.Role.EMPLEADO_TALLER)
      throw new IllegalArgumentException("El usuario no es un empleado de taller activo");
    return usuario;
  }

  private void verificarResponsable(ItemOrdenTrabajo tarea, User empleado) {
    if (tarea.empleadoAsignado == null || !tarea.empleadoAsignado.id.equals(empleado.id))
      throw new IllegalArgumentException("La tarea no está asignada a tu usuario");
  }

  private boolean esVisibleEnTaller(WorkOrder orden) {
    return orden.status == Enums.OrderStatus.RECEPCION || orden.status == Enums.OrderStatus.EN_PROCESO;
  }

  private VistaOrden crearVista(WorkOrder orden) {
    return new VistaOrden(orden.id, orden.orderNumber, orden.status, ApiDtos.vehicle(orden.vehicle), orden.cylinders,
        orden.finalMeasure, orden.receptionDescription, orden.items.stream().map(this::crearVistaTarea).toList());
  }

  private VistaTarea crearVistaTarea(ItemOrdenTrabajo item) {
    var empleado = item.empleadoAsignado == null ? null
        : new VistaEmpleado(item.empleadoAsignado.id, item.empleadoAsignado.name);
    var historial = item.historial.stream().map(evento -> new VistaHistorial(evento.fecha, evento.accion, evento.actor,
        evento.idEmpleado, evento.nombreEmpleado, evento.comentario)).toList();
    return new VistaTarea(item.id, item.descripcion, item.categoria, item.estadoTarea, empleado,
        item.notasTecnicas, historial);
  }

  private void agregarHistorial(ItemOrdenTrabajo tarea, Enums.AccionTareaTaller accion, User actor, User empleado,
      String comentario) {
    var evento = new HistorialTareaTaller();
    evento.itemOrdenTrabajo = tarea;
    evento.accion = accion;
    evento.actor = actor.name;
    evento.idEmpleado = empleado == null ? null : empleado.id;
    evento.nombreEmpleado = empleado == null ? null : empleado.name;
    evento.comentario = comentario == null || comentario.isBlank() ? null : comentario.trim();
    tarea.historial.add(evento);
  }

  private void agregarNotaTecnica(ItemOrdenTrabajo tarea, String nombreEmpleado, String comentario) {
    if (comentario == null || comentario.isBlank())
      return;
    String linea = "[" + Instant.now() + "] " + nombreEmpleado + ": " + comentario.trim();
    tarea.notasTecnicas = tarea.notasTecnicas == null || tarea.notasTecnicas.isBlank()
        ? linea : tarea.notasTecnicas + "\n" + linea;
  }
}
