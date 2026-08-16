package com.rectificadora.gestion.web;

import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.*;
import com.rectificadora.gestion.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
  private final ClientRepository clients;
  private final WorkOrderRepository orders;
  private final AuditService audit;

  ClientController(ClientRepository c, WorkOrderRepository o, AuditService a) {
    clients = c;
    orders = o;
    audit = a;
  }

  public record VehicleInput(UUID id, @NotBlank String description, String engineNumber, String licensePlate) {
  }

  public record ClientInput(@NotBlank String name, String phone, String email, String address,
      List<VehicleInput> vehicles) {
  }

  @GetMapping
  @Transactional
  public List<ApiDtos.ClientView> list(@RequestParam(defaultValue = "") String q) {
    return (q.isBlank() ? clients.findAll() : clients.findByNameContainingIgnoreCaseOrderByName(q)).stream()
        .map(ApiDtos::client).toList();
  }

  @GetMapping("/{id}")
  @Transactional
  public ApiDtos.ClientView get(@PathVariable UUID id) {
    return ApiDtos.client(clients.findById(id).orElseThrow());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public ApiDtos.ClientView create(@Valid @RequestBody ClientInput in) {
    var c = apply(new Client(), in);
    c = clients.save(c);
    audit.record("CREATE", "CLIENT", c.id, c.name);
    return ApiDtos.client(c);
  }

  @PutMapping("/{id}")
  @Transactional
  public ApiDtos.ClientView update(@PathVariable UUID id, @Valid @RequestBody ClientInput in) {
    var c = clients.findById(id).orElseThrow();
    apply(c, in);
    c = clients.save(c);
    audit.record("UPDATE", "CLIENT", c.id, c.name);
    return ApiDtos.client(c);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable UUID id) {
    var c = clients.findById(id).orElseThrow();
    if (orders.countByClientId(id) > 0)
      throw new IllegalArgumentException("No se puede eliminar un cliente con órdenes asociadas");
    clients.delete(c);
    audit.record("DELETE", "CLIENT", id, c.name);
  }

  private Client apply(Client c, ClientInput in) {
    c.name = in.name();
    c.phone = in.phone();
    c.email = in.email();
    c.address = in.address();
    var incoming = in.vehicles() == null ? List.<VehicleInput>of() : in.vehicles();
    var ids = incoming.stream().map(VehicleInput::id).filter(Objects::nonNull)
        .collect(java.util.stream.Collectors.toSet());
    c.vehicles.removeIf(v -> !ids.contains(v.id));
    for (var v : incoming) {
      var x = v.id() == null ? null
          : c.vehicles.stream().filter(current -> current.id.equals(v.id())).findFirst().orElse(null);
      if (x == null) {
        x = new Vehicle();
        x.client = c;
        c.vehicles.add(x);
      }
      x.description = v.description();
      x.engineNumber = v.engineNumber();
      x.licensePlate = v.licensePlate();
    }
    return c;
  }
}
