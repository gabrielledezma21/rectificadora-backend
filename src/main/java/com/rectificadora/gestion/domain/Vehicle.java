package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "vehicles")
public class Vehicle {
  @Id public UUID id = UUID.randomUUID();
  @JsonIgnore @ManyToOne(optional=false) @JoinColumn(name="client_id") public Client client;
  @Column(nullable=false) public String description;
  public String engineNumber;
  public String licensePlate;
  public Vehicle() {}
}
