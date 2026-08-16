package com.rectificadora.gestion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  private static final String ESQUEMA_SEGURIDAD = "bearerAuth";

  @Bean
  OpenAPI documentacionApi() {
    return new OpenAPI()
        .info(new Info()
            .title("API · Sistema de Gestión de Rectificadora")
            .version("1.0.0")
            .description("API REST para órdenes de trabajo, clientes, taller, pagos, usuarios, estadísticas, auditoría y respaldos.")
            .contact(new Contact().name("Rectificadora Las Flores")))
        .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD,
            new SecurityScheme()
                .name(ESQUEMA_SEGURIDAD)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
  }
}
