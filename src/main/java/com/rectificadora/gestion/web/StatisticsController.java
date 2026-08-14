package com.rectificadora.gestion.web;
import com.rectificadora.gestion.domain.WorkOrder; import com.rectificadora.gestion.repository.WorkOrderRepository; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal; import java.time.*; import java.util.*; import java.util.stream.Collectors;
@RestController @RequestMapping("/api/statistics") @PreAuthorize("hasRole('ADMIN')") public class StatisticsController {
  private final WorkOrderRepository repo;StatisticsController(WorkOrderRepository r){repo=r;}
  public record Summary(long totalOrders,Map<String,Long> byStatus,BigDecimal billed,BigDecimal collected,BigDecimal pending,Map<String,Long> topTasks){}
  @GetMapping public Summary summary(@RequestParam int year,@RequestParam int month){var from=YearMonth.of(year,month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();var to=YearMonth.of(year,month).plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();var list=repo.findByCreatedAtBetweenOrderByCreatedAtDesc(from,to);var by=list.stream().collect(Collectors.groupingBy(w->w.status.name(),Collectors.counting()));var billed=list.stream().map(w->w.total).reduce(BigDecimal.ZERO,BigDecimal::add);var collected=list.stream().map(w->w.paid).reduce(BigDecimal.ZERO,BigDecimal::add);var top=list.stream().flatMap(w->w.items.stream()).collect(Collectors.groupingBy(i->i.description,Collectors.counting()));return new Summary(list.size(),by,billed,collected,billed.subtract(collected),top);}
}
