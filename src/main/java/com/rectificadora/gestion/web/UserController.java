package com.rectificadora.gestion.web;
import com.rectificadora.gestion.domain.*; import com.rectificadora.gestion.repository.UserRepository; import com.rectificadora.gestion.service.AuditService;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/users") @PreAuthorize("hasRole('ADMIN')") public class UserController {
  private final UserRepository repo; private final PasswordEncoder encoder; private final AuditService audit; UserController(UserRepository r,PasswordEncoder e,AuditService a){repo=r;encoder=e;audit=a;}
  public record Input(@NotBlank String name,@Email String email,String password,@NotNull Enums.Role role,boolean active){}
  @GetMapping public List<User> list(){return repo.findAll();}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public User create(@Valid @RequestBody Input i){if(i.password()==null||i.password().length()<8)throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");var u=apply(new User(),i);u.passwordHash=encoder.encode(i.password());u=repo.save(u);audit.record("CREATE","USER",u.id,u.email);return u;}
  @PutMapping("/{id}") public User update(@PathVariable UUID id,@Valid @RequestBody Input i){var u=apply(repo.findById(id).orElseThrow(),i);if(i.password()!=null&&!i.password().isBlank())u.passwordHash=encoder.encode(i.password());u=repo.save(u);audit.record("UPDATE","USER",u.id,u.email);return u;}
  private User apply(User u,Input i){u.name=i.name();u.email=i.email().toLowerCase();u.role=i.role();u.active=i.active();return u;}
}
