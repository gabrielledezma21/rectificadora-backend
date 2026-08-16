package com.rectificadora.gestion.web;

import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.UserRepository;
import com.rectificadora.gestion.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
  private final UserRepository repo;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  UserController(UserRepository r, PasswordEncoder e, AuditService a) {
    repo = r;
    encoder = e;
    audit = a;
  }

  public record Input(@NotBlank String name, @Email String email, String password, @NotNull Enums.Role role,
      boolean active) {
  }

  @GetMapping
  public List<ApiDtos.UserView> list() {
    return repo.findAll().stream().map(ApiDtos::user).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiDtos.UserView create(@Valid @RequestBody Input i) {
    if (i.password() == null || i.password().length() < 8)
      throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
    var u = apply(new User(), i);
    u.passwordHash = encoder.encode(i.password());
    u = repo.save(u);
    audit.record("CREATE", "USER", u.id, u.email);
    return ApiDtos.user(u);
  }

  @PutMapping("/{id}")
  public ApiDtos.UserView update(@PathVariable UUID id, @Valid @RequestBody Input i) {
    var u = repo.findById(id).orElseThrow();
    if (u.role == Enums.Role.ADMIN && u.active && (i.role() != Enums.Role.ADMIN || !i.active())
        && repo.countByRoleAndActiveTrue(Enums.Role.ADMIN) <= 1)
      throw new IllegalArgumentException("Debe quedar al menos un administrador activo");
    apply(u, i);
    if (i.password() != null && !i.password().isBlank())
      u.passwordHash = encoder.encode(i.password());
    u = repo.save(u);
    audit.record("UPDATE", "USER", u.id, u.email);
    return ApiDtos.user(u);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, java.security.Principal principal) {
    var u = repo.findById(id).orElseThrow();
    if (u.email.equalsIgnoreCase(principal.getName()))
      throw new IllegalArgumentException("No podés eliminar tu propio usuario");
    if (u.role == Enums.Role.ADMIN && u.active && repo.countByRoleAndActiveTrue(Enums.Role.ADMIN) <= 1)
      throw new IllegalArgumentException("Debe quedar al menos un administrador activo");
    repo.delete(u);
    audit.record("DELETE", "USER", id, u.email);
  }

  private User apply(User u, Input i) {
    u.name = i.name();
    u.email = i.email().toLowerCase();
    u.role = i.role();
    u.active = i.active();
    return u;
  }
}
