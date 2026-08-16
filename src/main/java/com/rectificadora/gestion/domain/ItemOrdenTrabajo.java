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
public class ItemOrdenTrabajo {
  @Id
  public UUID id = UUID.randomUUID();

  @JsonIgnore
  @ManyToOne(optional = false)
  @JoinColumn(name = "work_order_id")
  public WorkOrder ordenTrabajo;

  @ManyToOne
  @JoinColumn(name = "catalog_task_id")
  public CatalogTask tareaCatalogo;

  @Column(name = "description", nullable = false)
  public String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  public Enums.TaskCategory categoria;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  public BigDecimal precioUnitario = BigDecimal.ZERO;

  @Column(name = "quantity", nullable = false)
  public int cantidad = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_status", nullable = false)
  public Enums.EstadoTareaTaller estadoTarea = Enums.EstadoTareaTaller.DISPONIBLE;

  @ManyToOne
  @JoinColumn(name = "assigned_employee_id")
  public User empleadoAsignado;

  @Column(name = "accepted_at")
  public Instant fechaAceptacion;

  @Column(name = "started_at")
  public Instant fechaInicio;

  @Column(name = "completed_at")
  public Instant fechaFinalizacion;

  @Column(name = "technical_notes", columnDefinition = "text")
  public String notasTecnicas;

  @OneToMany(mappedBy = "itemOrdenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("fecha ASC")
  public List<HistorialTareaTaller> historial = new ArrayList<>();

  @Version
  @Column(name = "version")
  public long version;

  public ItemOrdenTrabajo() {
  }
}
