package com.rectificadora.gestion.web;
import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.CatalogTaskRepository;
import com.rectificadora.gestion.service.AuditService;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal; import java.util.*;
@RestController @RequestMapping("/api/tasks") public class CatalogTaskController {
  private final CatalogTaskRepository repo; private final AuditService audit; CatalogTaskController(CatalogTaskRepository r,AuditService a){repo=r;audit=a;}
  public record Input(@NotBlank String name,@NotNull Enums.TaskCategory category,@NotNull @PositiveOrZero BigDecimal price,boolean active){}
  @GetMapping public List<CatalogTask> list(){return repo.findByActiveTrueOrderByCategoryAscNameAsc();}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public CatalogTask create(@Valid @RequestBody Input i){var t=apply(new CatalogTask(),i);t=repo.save(t);audit.record("CREATE","TASK",t.id,t.name);return t;}
  @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public CatalogTask update(@PathVariable UUID id,@Valid @RequestBody Input i){var t=apply(repo.findById(id).orElseThrow(),i);t=repo.save(t);audit.record("UPDATE","TASK",t.id,t.name);return t;}
  private CatalogTask apply(CatalogTask t,Input i){t.name=i.name();t.category=i.category();t.price=i.price();t.active=i.active();return t;}
}
