package com.rectificadora.gestion.config;

import com.rectificadora.gestion.domain.*;
import com.rectificadora.gestion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;

@Configuration
public class BootstrapData {
  @Bean CommandLineRunner seed(UserRepository users, CatalogTaskRepository tasks, PasswordEncoder encoder) {
    return args -> {
      if (users.count() == 0) { var u = new User(); u.name="Administrador"; u.email="admin@taller.com"; u.passwordHash=encoder.encode("admin123"); u.role=Enums.Role.ADMIN; users.save(u); }
      if (tasks.count() == 0) {
        add(tasks,"Encamisar",Enums.TaskCategory.BLOCK,15000); add(tasks,"Rectificar cilindros",Enums.TaskCategory.BLOCK,12000); add(tasks,"Bruñido",Enums.TaskCategory.BLOCK,8000); add(tasks,"Plano de tapa",Enums.TaskCategory.TAPA,7000); add(tasks,"Prueba de presión",Enums.TaskCategory.TAPA,3000); add(tasks,"Retenes",Enums.TaskCategory.REPUESTO,4000); add(tasks,"Pistones",Enums.TaskCategory.REPUESTO,18000); add(tasks,"Pulido de cigüeñal",Enums.TaskCategory.CIGUENAL,8000);
      }
    };
  }
  private void add(CatalogTaskRepository repo, String name, Enums.TaskCategory category, int price) { var t=new CatalogTask(); t.name=name; t.category=category; t.price=BigDecimal.valueOf(price); repo.save(t); }
}
