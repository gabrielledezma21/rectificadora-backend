package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
public class User {
  @Id public UUID id = UUID.randomUUID();
  @Column(nullable=false) public String name;
  @Column(nullable=false, unique=true) public String email;
  @JsonIgnore @Column(nullable=false) public String passwordHash;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public Enums.Role role = Enums.Role.OPERADOR;
  @Column(nullable=false) public boolean active = true;
  @Column(nullable=false) public Instant createdAt = Instant.now();
  public User() {}
}
