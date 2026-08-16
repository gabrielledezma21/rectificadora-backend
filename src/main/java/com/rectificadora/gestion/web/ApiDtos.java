package com.rectificadora.gestion.web;

import com.rectificadora.gestion.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

final class ApiDtos {
  private ApiDtos() {
  }

  record VehicleView(UUID id, String description, String engineNumber, String licensePlate) {
  }

  record ClientView(UUID id, String name, String phone, String email, String address, Instant createdAt,
      List<VehicleView> vehicles) {
  }

  record TaskView(UUID id, String name, Enums.TaskCategory category, BigDecimal price, boolean active) {
  }

  record EmployeeView(UUID id, String name) {
  }

  record TaskHistoryView(UUID id, Instant occurredAt, Enums.AccionTareaTaller action, String actor,
      UUID employeeId, String employeeName, String comment) {
  }

  record ItemView(UUID id, TaskView catalogTask, String description, Enums.TaskCategory category, BigDecimal unitPrice,
      int quantity, Enums.EstadoTareaTaller taskStatus, EmployeeView assignedEmployee, Instant acceptedAt,
      Instant startedAt, Instant completedAt, String technicalNotes, List<TaskHistoryView> history) {
  }

  record PaymentView(UUID id, Instant paidAt, BigDecimal amount, Enums.PaymentMethod method, String details,
      String registeredBy, Instant cancelledAt, String cancelledBy, String cancellationReason) {
  }

  record OrderView(UUID id, String orderNumber, Instant createdAt, LocalDate promisedDate, ClientView client,
      VehicleView vehicle, Enums.OrderStatus status, Integer cylinders, String finalMeasure,
      String receptionDescription, String notes, BigDecimal total, BigDecimal paid, BigDecimal balance,
      List<ItemView> items, List<PaymentView> payments, long version) {
  }

  record UserView(UUID id, String name, String email, Enums.Role role, Set<Enums.Permission> permissions,
      boolean active, Instant createdAt) {
  }

  static VehicleView vehicle(Vehicle v) {
    return v == null ? null : new VehicleView(v.id, v.description, v.engineNumber, v.licensePlate);
  }

  static ClientView client(Client c) {
    return new ClientView(c.id, c.name, c.phone, c.email, c.address, c.createdAt,
        c.vehicles.stream().map(ApiDtos::vehicle).toList());
  }

  static ClientView basicClient(Client c) {
    return new ClientView(c.id, c.name, c.phone, null, null, c.createdAt,
        c.vehicles.stream().map(ApiDtos::vehicle).toList());
  }

  static TaskView task(CatalogTask t) {
    return t == null ? null : new TaskView(t.id, t.name, t.category, t.price, t.active);
  }

  static ItemView item(ItemOrdenTrabajo i) {
    var employee = i.empleadoAsignado == null ? null : new EmployeeView(i.empleadoAsignado.id, i.empleadoAsignado.name);
    var history = i.historial.stream().map(h -> new TaskHistoryView(h.id, h.fecha, h.accion, h.actor,
        h.idEmpleado, h.nombreEmpleado, h.comentario)).toList();
    return new ItemView(i.id, task(i.tareaCatalogo), i.descripcion, i.categoria, i.precioUnitario, i.cantidad,
        i.estadoTarea, employee, i.fechaAceptacion, i.fechaInicio, i.fechaFinalizacion, i.notasTecnicas, history);
  }

  static PaymentView payment(Payment p) {
    return new PaymentView(p.id, p.paidAt, p.amount, p.method, p.details, p.registeredBy,
        p.cancelledAt, p.cancelledBy, p.cancellationReason);
  }

  static OrderView order(WorkOrder w) {
    return order(w, true);
  }

  static OrderView order(WorkOrder w, boolean sensitiveClientData) {
    return new OrderView(w.id, w.orderNumber, w.createdAt, w.promisedDate,
        sensitiveClientData ? client(w.client) : basicClient(w.client), vehicle(w.vehicle),
        w.status, w.cylinders, w.finalMeasure, w.receptionDescription, w.notes, w.total, w.paid, w.getBalance(),
        w.items.stream().map(ApiDtos::item).toList(), w.payments.stream().map(ApiDtos::payment).toList(), w.version);
  }

  static UserView user(User u) {
    return new UserView(u.id, u.name, u.email, u.role, u.effectivePermissions(), u.active, u.createdAt);
  }
}
