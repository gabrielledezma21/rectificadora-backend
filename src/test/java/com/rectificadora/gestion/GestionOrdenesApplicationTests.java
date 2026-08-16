package com.rectificadora.gestion;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GestionOrdenesApplicationTests {
  private static final String CLAVE_ALEATORIA = UUID.randomUUID().toString().replace("-", "")
      + UUID.randomUUID().toString().replace("-", "");

  @Autowired
  MockMvc mockMvc;

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add("app.jwt-secret", () -> CLAVE_ALEATORIA);
    registry.add("app.admin-email", () -> "admin@example.com");
    registry.add("app.admin-password", () -> CLAVE_ALEATORIA.substring(0, 24));
  }

  @Test
  void contextLoads() {
  }

  @Test
  void openApiEsPublica() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk());
  }

  @Test
  void swaggerEsPublico() throws Exception {
    mockMvc.perform(get("/swagger-ui.html"))
        .andExpect(status().is3xxRedirection());
  }
}
