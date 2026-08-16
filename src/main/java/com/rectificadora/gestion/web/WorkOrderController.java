package com.rectificadora.gestion.web;

import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.*;
import com.rectificadora.gestion.service.AuditService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class WorkOrderController {
  private final WorkOrderRepository orders;
  private final ClientRepository clients;
  private final VehicleRepository vehicles;
  private final CatalogTaskRepository tasks;
  private final AuditService audit;

  WorkOrderController(WorkOrderRepository o, ClientRepository c, VehicleRepository v, CatalogTaskRepository t,
      AuditService a) {
    orders = o;
    clients = c;
    vehicles = v;
    tasks = t;
    audit = a;
  }

  public record ItemInput(UUID taskId, @NotBlank String description, @NotNull Enums.TaskCategory category,
      @NotNull @PositiveOrZero BigDecimal unitPrice, @Min(1) int quantity) {
  }

  public record OrderInput(@NotNull UUID clientId, UUID vehicleId, LocalDate promisedDate,
      @NotNull Enums.OrderStatus status, Integer cylinders, String finalMeasure, String receptionDescription,
      String notes, List<ItemInput> items) {
  }

  public record PaymentInput(@NotNull @Positive BigDecimal amount, @NotNull Enums.PaymentMethod method,
      String details) {
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @Transactional
  public List<ApiDtos.OrderView> list(@RequestParam(defaultValue = "") String q, Authentication auth) {
    boolean sensitive = canViewClientHistory(auth);
    var source = (q.isBlank()
        ? orders.findAll().stream().sorted(Comparator.comparing((WorkOrder w) -> w.createdAt).reversed()).toList()
        : orders.search(q));
    return source.stream()
        .filter(w -> sensitive || isOperationallyVisible(w))
        .map(w -> ApiDtos.order(w, sensitive)).toList();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @Transactional
  public ApiDtos.OrderView get(@PathVariable UUID id, Authentication auth) {
    var order = orders.findById(id).orElseThrow();
    ensureCanView(order, auth);
    return ApiDtos.order(order, canViewClientHistory(auth));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public ApiDtos.OrderView create(@Valid @RequestBody OrderInput in, Authentication auth) {
    var w = apply(new WorkOrder(), in);
    w.orderNumber = nextNumber();
    w = orders.save(w);
    audit.record("CREATE", "ORDER", w.id, w.orderNumber);
    return ApiDtos.order(w, canViewClientHistory(auth));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @Transactional
  public ApiDtos.OrderView update(@PathVariable UUID id, @Valid @RequestBody OrderInput in, Authentication auth) {
    var w = orders.findById(id).orElseThrow();
    ensureCanView(w, auth);
    w.items.clear();
    apply(w, in);
    w = orders.save(w);
    audit.record("UPDATE", "ORDER", w.id, w.orderNumber);
    return ApiDtos.order(w, canViewClientHistory(auth));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_ORDENES_GESTIONAR')")
  @Transactional
  public ApiDtos.OrderView status(@PathVariable UUID id, @RequestParam Enums.OrderStatus value, Authentication auth) {
    var w = orders.findById(id).orElseThrow();
    ensureCanView(w, auth);
    w.status = value;
    w = orders.save(w);
    audit.record("STATUS", "ORDER", w.id, w.orderNumber + " -> " + value);
    return ApiDtos.order(w, canViewClientHistory(auth));
  }

  @PostMapping("/{id}/payments")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_PAGOS_REGISTRAR')")
  @Transactional
  public ApiDtos.OrderView pay(@PathVariable UUID id, @Valid @RequestBody PaymentInput in,
      java.security.Principal principal, Authentication auth) {
    var w = orders.findById(id).orElseThrow();
    ensureCanView(w, auth);
    if (in.amount().compareTo(w.getBalance()) > 0)
      throw new IllegalArgumentException("El pago supera el saldo pendiente");
    if (!isAdmin(auth) && in.method() != Enums.PaymentMethod.EFECTIVO)
      throw new IllegalArgumentException("El personal administrativo solo puede registrar pagos en efectivo");
    var previousBalance = w.getBalance();
    var p = new Payment();
    p.workOrder = w;
    p.amount = in.amount();
    p.method = in.method();
    p.details = in.details();
    p.registeredBy = principal.getName();
    w.payments.add(p);
    w.paid = w.paid.add(p.amount);
    w = orders.save(w);
    audit.record("PAYMENT", "ORDER", w.id, w.orderNumber + " $" + p.amount
        + " | saldo anterior $" + previousBalance + " | saldo restante $" + w.getBalance());
    return ApiDtos.order(w, canViewClientHistory(auth));
  }

  public record CancellationInput(@NotBlank String reason) {
  }

  @PatchMapping("/{orderId}/payments/{paymentId}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public ApiDtos.OrderView cancelPayment(@PathVariable UUID orderId, @PathVariable UUID paymentId,
      @Valid @RequestBody CancellationInput input, java.security.Principal principal) {
    var order = orders.findById(orderId).orElseThrow();
    var payment = order.payments.stream().filter(p -> p.id.equals(paymentId)).findFirst().orElseThrow();
    if (payment.cancelledAt != null)
      throw new IllegalArgumentException("El pago ya fue anulado");
    payment.cancelledAt = Instant.now();
    payment.cancelledBy = principal.getName();
    payment.cancellationReason = input.reason();
    order.paid = order.paid.subtract(payment.amount);
    order = orders.save(order);
    audit.record("CANCEL_PAYMENT", "ORDER", order.id,
        order.orderNumber + " $" + payment.amount + " | motivo: " + input.reason());
    return ApiDtos.order(order);
  }

  private WorkOrder apply(WorkOrder w, OrderInput in) {
    w.client = clients.findById(in.clientId()).orElseThrow();
    w.vehicle = in.vehicleId() == null ? null : vehicles.findById(in.vehicleId()).orElseThrow();
    w.promisedDate = in.promisedDate();
    w.status = in.status();
    w.cylinders = in.cylinders();
    w.finalMeasure = in.finalMeasure();
    w.receptionDescription = in.receptionDescription();
    w.notes = in.notes();
    BigDecimal total = BigDecimal.ZERO;
    if (in.items() != null)
      for (var i : in.items()) {
        var x = new WorkOrderItem();
        x.workOrder = w;
        x.catalogTask = i.taskId() == null ? null : tasks.findById(i.taskId()).orElse(null);
        x.description = i.description();
        x.category = i.category();
        x.unitPrice = i.unitPrice();
        x.quantity = i.quantity();
        w.items.add(x);
        total = total.add(i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())));
      }
    w.total = total;
    return w;
  }

  private String nextNumber() {
    int year = Year.now().getValue();
    long seq = orders.nextOrderSequence();
    return "OT-" + year + "-" + String.format("%05d", seq);
  }

  private boolean canViewClientHistory(Authentication auth) {
    return isAdmin(auth) || auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("PERM_CLIENTES_VER_HISTORIAL"));
  }

  private boolean isAdmin(Authentication auth) {
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  private boolean isOperationallyVisible(WorkOrder order) {
    var zone = ZoneId.of("America/Argentina/Buenos_Aires");
    var monthStart = YearMonth.now(zone).atDay(1).atStartOfDay(zone).toInstant();
    return !order.createdAt.isBefore(monthStart)
        || order.status == Enums.OrderStatus.RECEPCION
        || order.status == Enums.OrderStatus.EN_PROCESO
        || order.status == Enums.OrderStatus.FINALIZADO;
  }

  private void ensureCanView(WorkOrder order, Authentication auth) {
    if (!canViewClientHistory(auth) && !isOperationallyVisible(order))
      throw new AccessDeniedException("No tenés permiso para consultar el historial de esta orden");
  }
}
