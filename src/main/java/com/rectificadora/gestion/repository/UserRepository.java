package com.rectificadora.gestion.repository;
import com.rectificadora.gestion.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);
  long countByRoleAndActiveTrue(com.rectificadora.gestion.domain.Enums.Role role);
}
