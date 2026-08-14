package com.rectificadora.gestion.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="catalog_tasks")
public class CatalogTask {
  @Id public UUID id = UUID.randomUUID();
  @Column(nullable=false) public String name;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public Enums.TaskCategory category;
  @Column(nullable=false, precision=14, scale=2) public BigDecimal price = BigDecimal.ZERO;
  @Column(nullable=false) public boolean active = true;
  public CatalogTask() {}
}
