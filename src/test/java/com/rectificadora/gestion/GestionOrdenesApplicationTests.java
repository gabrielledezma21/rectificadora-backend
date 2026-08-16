package com.rectificadora.gestion;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class GestionOrdenesApplicationTests {
  private static final String CLAVE_ALEATORIA = UUID.randomUUID().toString().replace("-", "")
      + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("app.jwt-secret", () -> CLAVE_ALEATORIA);
    registry.add("app.admin-email", () -> "admin@example.com");
    registry.add("app.admin-password", () -> CLAVE_ALEATORIA.substring(0, 24));
  }

  @Test
  void contextLoads() {
  }
}
