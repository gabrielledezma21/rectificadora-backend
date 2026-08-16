package com.rectificadora.gestion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

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
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD))
        .tags(List.of(
            new Tag().name("Autenticación").description("Inicio de sesión y protección CSRF."),
            new Tag().name("Órdenes").description("Gestión administrativa de órdenes de trabajo, estados y pagos."),
            new Tag().name("Clientes").description("Clientes, vehículos e historial relacionado."),
            new Tag().name("Taller").description("Tablero, tareas, asignaciones e historial de empleados."),
            new Tag().name("Catálogo").description("Trabajos disponibles y precios del taller."),
            new Tag().name("Usuarios").description("Usuarios, roles y permisos."),
            new Tag().name("Estadísticas").description("Indicadores mensuales del negocio."),
            new Tag().name("Auditoría").description("Registro de operaciones realizadas en el sistema."),
            new Tag().name("Respaldos").description("Exportación y herramientas administrativas de respaldo."),
            new Tag().name("Sistema").description("Estado general del servicio.")));
  }

  @Bean
  OpenApiCustomizer organizarOperaciones() {
    return openApi -> openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(operacion -> {
      operacion.setTags(List.of(etiquetaPara(ruta)));
      if (esRutaPublica(ruta))
        operacion.setSecurity(Collections.emptyList());
    }));
  }

  private String etiquetaPara(String ruta) {
    if (ruta.startsWith("/api/auth"))
      return "Autenticación";
    if (ruta.startsWith("/api/orders"))
      return "Órdenes";
    if (ruta.startsWith("/api/clients"))
      return "Clientes";
    if (ruta.startsWith("/api/workshop"))
      return "Taller";
    if (ruta.startsWith("/api/tasks"))
      return "Catálogo";
    if (ruta.startsWith("/api/users"))
      return "Usuarios";
    if (ruta.startsWith("/api/statistics"))
      return "Estadísticas";
    if (ruta.startsWith("/api/audit"))
      return "Auditoría";
    if (ruta.startsWith("/api/backups"))
      return "Respaldos";
    return "Sistema";
  }

  private boolean esRutaPublica(String ruta) {
    return ruta.equals("/api/health") || ruta.startsWith("/api/auth");
  }
}
