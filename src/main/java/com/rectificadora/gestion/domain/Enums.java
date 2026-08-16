package com.rectificadora.gestion.domain;

public final class Enums {
  private Enums() {
  }

  public enum Role {
    ADMIN, OPERADOR
  }

  public enum Permission {
    ORDENES_GESTIONAR, CLIENTES_DATOS_BASICOS, CLIENTES_VER_HISTORIAL, CATALOGO_GESTIONAR,
    PAGOS_REGISTRAR, FINANZAS_VER, ESTADISTICAS_VER, AUDITORIA_VER, USUARIOS_GESTIONAR,
    RESPALDOS_GESTIONAR
  }

  public enum OrderStatus {
    RECEPCION, EN_PROCESO, FINALIZADO, ENTREGADO, CANCELADO
  }

  public enum TaskCategory {
    BLOCK, REPUESTO, TAPA, CIGUENAL, OTRO
  }

  public enum PaymentMethod {
    EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE, OTRO
  }
}
