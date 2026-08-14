package com.rectificadora.gestion.repository;
import com.rectificadora.gestion.domain.WorkOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
  Optional<WorkOrder> findByOrderNumber(String number);
  @Query("select distinct w from WorkOrder w left join fetch w.items left join fetch w.payments where w.id=:id") Optional<WorkOrder> findDetailedById(@Param("id") UUID id);
  @Query("select w from WorkOrder w where lower(w.client.name) like lower(concat('%',:q,'%')) or lower(w.orderNumber) like lower(concat('%',:q,'%')) order by w.createdAt desc") List<WorkOrder> search(@Param("q") String q);
  List<WorkOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
