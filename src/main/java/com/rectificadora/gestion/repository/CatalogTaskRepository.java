package com.rectificadora.gestion.repository;

import com.rectificadora.gestion.domain.CatalogTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CatalogTaskRepository extends JpaRepository<CatalogTask, UUID> {
    List<CatalogTask> findByActiveTrueOrderByCategoryAscNameAsc();
    List<CatalogTask> findAllByOrderByCategoryAscNameAsc();
}
