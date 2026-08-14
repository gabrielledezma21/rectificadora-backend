package com.rectificadora.gestion.repository;
import com.rectificadora.gestion.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ClientRepository extends JpaRepository<Client, UUID> { List<Client> findByNameContainingIgnoreCaseOrderByName(String name); }
