package com.rectificadora.gestion.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "clients")
public class Client {
  @Id public UUID id = UUID.randomUUID();
  @Column(nullable=false) public String name;
  public String phone;
  public String email;
  public String address;
  @Column(nullable=false) public Instant createdAt = Instant.now();
  @OneToMany(mappedBy="client", cascade=CascadeType.ALL, orphanRemoval=true)
  public List<Vehicle> vehicles = new ArrayList<>();
  public Client() {}
}
