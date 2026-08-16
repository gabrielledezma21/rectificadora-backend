package com.rectificadora.gestion.config;

import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Profile("demo")
public class DatosDemostracionConfig {
  @Bean
  @Order(2)
  CommandLineRunner cargarDatosDemostracion(ClientRepository clientes, WorkOrderRepository ordenes,
      CatalogTaskRepository tareas, AuditLogRepository auditoria) {
    return args -> {
      if (clientes.count() > 0 || ordenes.count() > 0)
        return;

      var clienteUno = cliente("Cliente Demo 1", "0000-0001", "cliente1@example.com", "Localidad Demo",
          "Motor Demo 1.8", "MOTOR-DEMO-001", "DEMO-001");
      var clienteDos = cliente("Cliente Demo 2", "0000-0002", "cliente2@example.com", "Localidad Demo",
          "Motor Demo 1.4", "MOTOR-DEMO-002", "DEMO-002");
      var clienteTres = cliente("Cliente Demo 3", "0000-0003", "cliente3@example.com", "Localidad Demo",
          "Motor Demo 3.0", "MOTOR-DEMO-003", "DEMO-003");
      var tallerDemo = cliente("Taller Demo", "0000-0004", "taller@example.com", "Localidad Demo", "Motor Demo 1.6",
          "MOTOR-DEMO-004", "DEMO-004");
      clientes.saveAll(List.of(clienteUno, clienteDos, clienteTres, tallerDemo));

      Map<String, CatalogTask> catalogo = tareas.findByActiveTrueOrderByCategoryAscNameAsc().stream()
          .collect(Collectors.toMap(t -> t.name, Function.identity()));

      var ejemplos = List.of(
          orden("OT-DEMO-00001", clienteUno, Enums.OrderStatus.RECEPCION, 1, 480000, 100000,
              List.of("Plano de tapa", "Prueba de presión", "Retenes"), catalogo),
          orden("OT-DEMO-00002", clienteDos, Enums.OrderStatus.EN_PROCESO, 3, 315000, 150000,
              List.of("Bruñido", "Pistones"), catalogo),
          orden("OT-DEMO-00003", clienteTres, Enums.OrderStatus.FINALIZADO, 8, 620000, 620000,
              List.of("Encamisar", "Rectificar cilindros", "Pistones"), catalogo),
          orden("OT-DEMO-00004", tallerDemo, Enums.OrderStatus.EN_PROCESO, 12, 215000, 80000,
              List.of("Plano de tapa", "Pulido de cigüeñal"), catalogo),
          orden("OT-DEMO-00005", clienteUno, Enums.OrderStatus.ENTREGADO, 32, 185000, 185000,
              List.of("Bruñido", "Retenes"), catalogo));
      ordenes.saveAll(ejemplos);

      var registro = new AuditLog();
      registro.username = "Sistema";
      registro.action = "SEED";
      registro.entityType = "DEMO";
      registro.detail = "Se cargaron clientes, vehículos, órdenes y pagos de demostración";
      auditoria.save(registro);
    };
  }

  private Client cliente(String nombre, String telefono, String email, String direccion,
      String descripcionVehiculo, String numeroMotor, String patente) {
    var cliente = new Client();
    cliente.name = nombre;
    cliente.phone = telefono;
    cliente.email = email;
    cliente.address = direccion;
    var vehiculo = new Vehicle();
    vehiculo.client = cliente;
    vehiculo.description = descripcionVehiculo;
    vehiculo.engineNumber = numeroMotor;
    vehiculo.licensePlate = patente;
    cliente.vehicles.add(vehiculo);
    return cliente;
  }

  private WorkOrder orden(String numero, Client cliente, Enums.OrderStatus estado, int diasAtras,
      long total, long pagado, List<String> nombresTareas, Map<String, CatalogTask> catalogo) {
    var orden = new WorkOrder();
    orden.orderNumber = numero;
    orden.client = cliente;
    orden.vehicle = cliente.vehicles.getFirst();
    orden.createdAt = Instant.now().minus(Duration.ofDays(diasAtras));
    orden.promisedDate = LocalDate.now().plusDays(Math.max(1, 10 - diasAtras));
    orden.status = estado;
    orden.cylinders = 4;
    orden.receptionDescription = "Piezas recibidas y verificadas en mostrador.";
    orden.notes = "Datos generados para demostración";
    orden.total = BigDecimal.valueOf(total);
    orden.paid = BigDecimal.valueOf(pagado);
    for (String nombre : nombresTareas) {
      var catalogada = catalogo.get(nombre);
      var item = new WorkOrderItem();
      item.workOrder = orden;
      item.catalogTask = catalogada;
      item.description = nombre;
      item.category = catalogada == null ? Enums.TaskCategory.OTRO : catalogada.category;
      item.unitPrice = catalogada == null ? BigDecimal.ZERO : catalogada.price;
      item.quantity = 1;
      orden.items.add(item);
    }
    if (pagado > 0) {
      var pago = new Payment();
      pago.workOrder = orden;
      pago.amount = BigDecimal.valueOf(pagado);
      pago.method = Enums.PaymentMethod.EFECTIVO;
      pago.details = "Pago de demostración";
      pago.registeredBy = "Sistema";
      pago.paidAt = orden.createdAt;
      orden.payments.add(pago);
    }
    return orden;
  }
}
