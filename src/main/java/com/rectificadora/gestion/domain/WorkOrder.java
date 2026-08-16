package com.rectificadora.gestion.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "work_orders")
public class WorkOrder {
  @Id
  public UUID id = UUID.randomUUID();
  @Column(nullable = false, unique = true)
  public String orderNumber;
  @Column(nullable = false)
  public Instant createdAt = Instant.now();
  public LocalDate promisedDate;
  @ManyToOne(optional = false)
  @JoinColumn(name = "client_id")
  public Client client;
  @ManyToOne
  @JoinColumn(name = "vehicle_id")
  public Vehicle vehicle;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Enums.OrderStatus status = Enums.OrderStatus.RECEPCION;
  public Integer cylinders;
  public String finalMeasure;
  @Column(columnDefinition = "text")
  public String receptionDescription;
  @Column(columnDefinition = "text")
  public String notes;
  @Column(nullable = false, precision = 14, scale = 2)
  public BigDecimal total = BigDecimal.ZERO;
  @Column(nullable = false, precision = 14, scale = 2)
  public BigDecimal paid = BigDecimal.ZERO;
  @OneToMany(mappedBy = "ordenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true)
  public List<ItemOrdenTrabajo> items = new ArrayList<>();
  @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  public List<Payment> payments = new ArrayList<>();
  @Version
  public long version;

  public WorkOrder() {
  }

  @Transient
  public BigDecimal getBalance() {
    return total.subtract(paid);
  }
}
