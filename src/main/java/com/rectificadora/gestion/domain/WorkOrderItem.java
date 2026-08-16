package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "work_order_items")
public class WorkOrderItem {
  @Id
  public UUID id = UUID.randomUUID();
  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "work_order_id")
  public WorkOrder workOrder;
  @ManyToOne
  @JoinColumn(name = "catalog_task_id")
  public CatalogTask catalogTask;
  @Column(nullable = false)
  public String description;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Enums.TaskCategory category;
  @Column(nullable = false, precision = 14, scale = 2)
  public BigDecimal unitPrice = BigDecimal.ZERO;
  @Column(nullable = false)
  public int quantity = 1;

  public WorkOrderItem() {
  }
}
