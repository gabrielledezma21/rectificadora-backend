package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_task_history")
public class HistorialTareaTaller {
  @Id
  public UUID id = UUID.randomUUID();

  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "work_order_item_id")
  public ItemOrdenTrabajo itemOrdenTrabajo;

  @Column(name = "occurred_at", nullable = false)
  public Instant fecha = Instant.now();

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false)
  public Enums.AccionTareaTaller accion;

  @Column(name = "actor", nullable = false)
  public String actor;

  @Column(name = "employee_id")
  public UUID idEmpleado;

  @Column(name = "employee_name")
  public String nombreEmpleado;

  @Column(name = "comment", columnDefinition = "text")
  public String comentario;

  public HistorialTareaTaller() {
  }
}
