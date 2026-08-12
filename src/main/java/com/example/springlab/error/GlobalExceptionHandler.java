package com.example.springlab.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    log.warn("[Validation error] " + ex.getMessage(), ex);

    return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "入力値が不正です"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("[Unexpected error] " + ex.getMessage(), ex);

    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "サーバーエラーが発生しました"));
  }
}
