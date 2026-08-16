package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
  @Id
  public UUID id = UUID.randomUUID();
  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "work_order_id")
  public WorkOrder workOrder;
  @Column(nullable = false)
  public Instant paidAt = Instant.now();
  @Column(nullable = false, precision = 14, scale = 2)
  public BigDecimal amount;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Enums.PaymentMethod method;
  public String details;
  @Column(nullable = false)
  public String registeredBy;
  public Instant cancelledAt;
  public String cancelledBy;
  public String cancellationReason;

  public Payment() {
  }
}
