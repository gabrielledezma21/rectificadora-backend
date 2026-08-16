package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_task_history")
public class WorkTaskHistory {
  @Id
  public UUID id = UUID.randomUUID();
  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "work_order_item_id")
  public WorkOrderItem workOrderItem;
  @Column(nullable = false)
  public Instant occurredAt = Instant.now();
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Enums.WorkTaskAction action;
  @Column(nullable = false)
  public String actor;
  public UUID employeeId;
  public String employeeName;
  @Column(columnDefinition = "text")
  public String comment;

  public WorkTaskHistory() {
  }
}
