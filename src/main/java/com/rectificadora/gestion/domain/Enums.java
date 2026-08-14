package com.rectificadora.gestion.domain;

public final class Enums {
  private Enums() {}
  public enum Role { ADMIN, OPERADOR }
  public enum OrderStatus { RECEPCION, EN_PROCESO, FINALIZADO, ENTREGADO, CANCELADO }
  public enum TaskCategory { BLOCK, REPUESTO, TAPA, CIGUENAL, OTRO }
  public enum PaymentMethod { EFECTIVO, TRANSFERENCIA, TARJETA, CHEQUE, OTRO }
}
