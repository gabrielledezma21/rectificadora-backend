package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Enums.WorkTaskStatus taskStatus = Enums.WorkTaskStatus.DISPONIBLE;
  @ManyToOne
  @JoinColumn(name = "assigned_employee_id")
  public User assignedEmployee;
  public Instant acceptedAt;
  public Instant startedAt;
  public Instant completedAt;
  @Column(columnDefinition = "text")
  public String technicalNotes;
  @OneToMany(mappedBy = "workOrderItem", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("occurredAt ASC")
  public List<WorkTaskHistory> history = new ArrayList<>();
  @Version
  public long version;

  public WorkOrderItem() {
  }
}
