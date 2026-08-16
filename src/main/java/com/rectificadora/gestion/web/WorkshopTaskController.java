package com.rectificadora.gestion.web;

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
public class WorkshopTaskController {
  private static final Set<Enums.WorkTaskStatus> ACTIVE_STATUSES =
      EnumSet.of(Enums.WorkTaskStatus.ACEPTADA, Enums.WorkTaskStatus.EN_PROCESO);

  private final WorkOrderRepository orders;
  private final WorkOrderItemRepository items;
  private final UserRepository users;
  private final AuditService audit;

  WorkshopTaskController(WorkOrderRepository orders, WorkOrderItemRepository items, UserRepository users,
      AuditService audit) {
    this.orders = orders;
    this.items = items;
    this.users = users;
    this.audit = audit;
  }

  record EmployeeView(UUID id, String name) {
  }

  record HistoryView(Instant occurredAt, Enums.WorkTaskAction action, String actor, UUID employeeId,
      String employeeName, String comment) {
  }

  record TaskView(UUID id, String description, Enums.TaskCategory category, Enums.WorkTaskStatus status,
      EmployeeView assignedEmployee, String technicalNotes, List<HistoryView> history) {
  }

  record OrderView(UUID id, String orderNumber, Enums.OrderStatus status, ApiDtos.VehicleView vehicle,
      Integer cylinders, String finalMeasure, String receptionDescription, List<TaskView> tasks) {
  }

  record CommentInput(String comment) {
  }

  record PendingInput(@NotBlank String reason) {
  }

  record AssignmentInput(UUID employeeId, String comment) {
  }

  @GetMapping("/orders")
  @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLEADO_TALLER') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @Transactional
  public List<OrderView> list() {
    return orders.findAll().stream()
        .filter(this::isVisibleInWorkshop)
        .sorted(Comparator.comparing((WorkOrder order) -> order.createdAt).reversed())
        .map(this::view).toList();
  }

  @PatchMapping("/tasks/{id}/accept")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public OrderView accept(@PathVariable UUID id, java.security.Principal principal) {
    var employee = currentEmployee(principal);
    if (items.existsByAssignedEmployeeIdAndTaskStatusIn(employee.id, ACTIVE_STATUSES))
      throw new IllegalArgumentException("Ya tenés una tarea activa. Finalizala o dejala pendiente antes de aceptar otra");
    var task = lockedTask(id);
    if (!isVisibleInWorkshop(task.workOrder))
      throw new IllegalArgumentException("La orden ya no está disponible para el taller");
    if (task.taskStatus == Enums.WorkTaskStatus.ASIGNADA
        && (task.assignedEmployee == null || !task.assignedEmployee.id.equals(employee.id)))
      throw new IllegalArgumentException("La tarea está reservada para otro empleado");
    if (task.taskStatus != Enums.WorkTaskStatus.DISPONIBLE
        && task.taskStatus != Enums.WorkTaskStatus.PENDIENTE
        && task.taskStatus != Enums.WorkTaskStatus.ASIGNADA)
      throw new IllegalArgumentException("La tarea ya no está disponible");
    var action = task.taskStatus == Enums.WorkTaskStatus.PENDIENTE
        ? Enums.WorkTaskAction.RETOMADA : Enums.WorkTaskAction.ACEPTADA;
    task.assignedEmployee = employee;
    task.taskStatus = Enums.WorkTaskStatus.ACEPTADA;
    task.acceptedAt = Instant.now();
    addHistory(task, action, employee, employee, null);
    items.save(task);
    audit.record("ACCEPT_TASK", "ORDER_TASK", task.id, task.description + " · " + employee.name);
    return view(task.workOrder);
  }

  @PatchMapping("/tasks/{id}/start")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public OrderView start(@PathVariable UUID id, java.security.Principal principal) {
    var employee = currentEmployee(principal);
    var task = lockedTask(id);
    ensureOwner(task, employee);
    if (task.taskStatus != Enums.WorkTaskStatus.ACEPTADA)
      throw new IllegalArgumentException("Primero debés aceptar la tarea");
    task.taskStatus = Enums.WorkTaskStatus.EN_PROCESO;
    task.startedAt = Instant.now();
    if (task.workOrder.status == Enums.OrderStatus.RECEPCION)
      task.workOrder.status = Enums.OrderStatus.EN_PROCESO;
    addHistory(task, Enums.WorkTaskAction.INICIADA, employee, employee, null);
    items.save(task);
    audit.record("START_TASK", "ORDER_TASK", task.id, task.description + " · " + employee.name);
    return view(task.workOrder);
  }

  @PatchMapping("/tasks/{id}/pending")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public OrderView pending(@PathVariable UUID id, @Valid @RequestBody PendingInput input,
      java.security.Principal principal) {
    var employee = currentEmployee(principal);
    var task = lockedTask(id);
    ensureOwner(task, employee);
    if (!ACTIVE_STATUSES.contains(task.taskStatus))
      throw new IllegalArgumentException("La tarea no está activa");
    addTechnicalNote(task, employee.name, input.reason());
    addHistory(task, Enums.WorkTaskAction.PAUSADA, employee, employee, input.reason());
    task.taskStatus = Enums.WorkTaskStatus.PENDIENTE;
    task.assignedEmployee = null;
    items.save(task);
    audit.record("PENDING_TASK", "ORDER_TASK", task.id, task.description + " · " + input.reason());
    return view(task.workOrder);
  }

  @PatchMapping("/tasks/{id}/complete")
  @PreAuthorize("hasRole('EMPLEADO_TALLER')")
  @Transactional
  public OrderView complete(@PathVariable UUID id, @RequestBody(required = false) CommentInput input,
      java.security.Principal principal) {
    var employee = currentEmployee(principal);
    var task = lockedTask(id);
    ensureOwner(task, employee);
    if (task.taskStatus != Enums.WorkTaskStatus.EN_PROCESO)
      throw new IllegalArgumentException("La tarea debe estar en proceso para poder finalizarla");
    String comment = input == null ? null : input.comment();
    addTechnicalNote(task, employee.name, comment);
    task.taskStatus = Enums.WorkTaskStatus.FINALIZADA;
    task.completedAt = Instant.now();
    addHistory(task, Enums.WorkTaskAction.FINALIZADA, employee, employee, comment);
    items.save(task);
    audit.record("COMPLETE_TASK", "ORDER_TASK", task.id, task.description + " · " + employee.name);
    return view(task.workOrder);
  }

  @PatchMapping("/tasks/{id}/assignment")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public OrderView assign(@PathVariable UUID id, @RequestBody AssignmentInput input,
      java.security.Principal principal) {
    var actor = users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    var task = lockedTask(id);
    if (task.taskStatus == Enums.WorkTaskStatus.FINALIZADA)
      throw new IllegalArgumentException("Reabrí la tarea antes de cambiar su responsable");
    var previous = task.assignedEmployee;
    if (input.employeeId() == null) {
      task.assignedEmployee = null;
      task.taskStatus = Enums.WorkTaskStatus.DISPONIBLE;
      addHistory(task, Enums.WorkTaskAction.LIBERADA, actor, previous, input.comment());
    } else {
      var employee = users.findById(input.employeeId()).orElseThrow();
      if (!employee.active || employee.role != Enums.Role.EMPLEADO_TALLER)
        throw new IllegalArgumentException("El responsable debe ser un empleado de taller activo");
      task.assignedEmployee = employee;
      task.taskStatus = Enums.WorkTaskStatus.ASIGNADA;
      task.acceptedAt = null;
      task.startedAt = null;
      addHistory(task, previous == null ? Enums.WorkTaskAction.ASIGNADA : Enums.WorkTaskAction.REASIGNADA,
          actor, employee, input.comment());
    }
    items.save(task);
    audit.record("ASSIGN_TASK", "ORDER_TASK", task.id,
        task.description + " · " + (task.assignedEmployee == null ? "sin responsable" : task.assignedEmployee.name));
    return view(task.workOrder);
  }

  @PatchMapping("/tasks/{id}/reopen")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public OrderView reopen(@PathVariable UUID id, @RequestBody(required = false) CommentInput input,
      java.security.Principal principal) {
    var actor = users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    var task = lockedTask(id);
    if (task.taskStatus != Enums.WorkTaskStatus.FINALIZADA)
      throw new IllegalArgumentException("Solo se pueden reabrir tareas finalizadas");
    task.taskStatus = Enums.WorkTaskStatus.DISPONIBLE;
    task.assignedEmployee = null;
    task.completedAt = null;
    addHistory(task, Enums.WorkTaskAction.REABIERTA, actor, null, input == null ? null : input.comment());
    items.save(task);
    audit.record("REOPEN_TASK", "ORDER_TASK", task.id, task.description);
    return view(task.workOrder);
  }

  private WorkOrderItem lockedTask(UUID id) {
    return items.findLockedById(id).orElseThrow();
  }

  private User currentEmployee(java.security.Principal principal) {
    var user = users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    if (!user.active || user.role != Enums.Role.EMPLEADO_TALLER)
      throw new IllegalArgumentException("El usuario no es un empleado de taller activo");
    return user;
  }

  private void ensureOwner(WorkOrderItem task, User employee) {
    if (task.assignedEmployee == null || !task.assignedEmployee.id.equals(employee.id))
      throw new IllegalArgumentException("La tarea no está asignada a tu usuario");
  }

  private boolean isVisibleInWorkshop(WorkOrder order) {
    return order.status == Enums.OrderStatus.RECEPCION || order.status == Enums.OrderStatus.EN_PROCESO;
  }

  private OrderView view(WorkOrder order) {
    return new OrderView(order.id, order.orderNumber, order.status, ApiDtos.vehicle(order.vehicle), order.cylinders,
        order.finalMeasure, order.receptionDescription, order.items.stream().map(this::taskView).toList());
  }

  private TaskView taskView(WorkOrderItem item) {
    var employee = item.assignedEmployee == null ? null
        : new EmployeeView(item.assignedEmployee.id, item.assignedEmployee.name);
    var history = item.history.stream().map(event -> new HistoryView(event.occurredAt, event.action, event.actor,
        event.employeeId, event.employeeName, event.comment)).toList();
    return new TaskView(item.id, item.description, item.category, item.taskStatus, employee,
        item.technicalNotes, history);
  }

  private void addHistory(WorkOrderItem task, Enums.WorkTaskAction action, User actor, User employee,
      String comment) {
    var event = new WorkTaskHistory();
    event.workOrderItem = task;
    event.action = action;
    event.actor = actor.name;
    event.employeeId = employee == null ? null : employee.id;
    event.employeeName = employee == null ? null : employee.name;
    event.comment = comment == null || comment.isBlank() ? null : comment.trim();
    task.history.add(event);
  }

  private void addTechnicalNote(WorkOrderItem task, String employeeName, String comment) {
    if (comment == null || comment.isBlank())
      return;
    String line = "[" + Instant.now() + "] " + employeeName + ": " + comment.trim();
    task.technicalNotes = task.technicalNotes == null || task.technicalNotes.isBlank()
        ? line : task.technicalNotes + "\n" + line;
  }
}
