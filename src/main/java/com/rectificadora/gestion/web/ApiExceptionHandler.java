package com.rectificadora.gestion.web;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  record ErrorResponse(Instant timestamp, int status, String error, String message) {
  }

  @ExceptionHandler({ NoSuchElementException.class })
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ErrorResponse notFound(Exception e) {
    return new ErrorResponse(Instant.now(), 404, "Not Found", "Registro no encontrado");
  }

  @ExceptionHandler({ IllegalArgumentException.class })
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse badRequest(Exception e) {
    return new ErrorResponse(Instant.now(), 400, "Bad Request", e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse validation(MethodArgumentNotValidException e) {
    String m = e.getBindingResult().getFieldErrors().stream().map(x -> x.getField() + ": " + x.getDefaultMessage())
        .findFirst().orElse("Datos inválidos");
    return new ErrorResponse(Instant.now(), 400, "Validation Error", m);
  }
}
