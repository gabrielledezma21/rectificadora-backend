package com.rectificadora.gestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
  @Id
  public UUID id = UUID.randomUUID();
  @Column(nullable = false)
  public String name;
  @Column(nullable = false, unique = true)
  public String email;
  @JsonIgnore
  @Column(nullable = false)
  public String passwordHash;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Enums.Role role = Enums.Role.OPERADOR;
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "permission", nullable = false)
  public Set<Enums.Permission> permissions = EnumSet.noneOf(Enums.Permission.class);
  @Column(nullable = false)
  public boolean active = true;
  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  public User() {
  }

  public Set<Enums.Permission> effectivePermissions() {
    if (role == Enums.Role.ADMIN)
      return EnumSet.allOf(Enums.Permission.class);
    return permissions.isEmpty() ? EnumSet.noneOf(Enums.Permission.class) : EnumSet.copyOf(permissions);
  }
}
