package com.example.springlab.error;

import com.example.springlab.item.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    log.warn("[Validation error] " + ex.getMessage(), ex);

    return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "入力値が不正です"));
  }

  // パス変数の型変換失敗（例: /items/{id} に数値でない値）→ 400。元 NestJS の ParseUUIDPipe 相当。
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    log.warn("[Type mismatch] " + ex.getMessage(), ex);

    return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "入力値が不正です"));
  }

  // 商品が存在しない場合 → 404。元 NestJS の NotFoundException('商品が存在しません') 相当。
  @ExceptionHandler(ItemNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleItemNotFound(ItemNotFoundException ex) {
    log.warn("[Not found] " + ex.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("[Unexpected error] " + ex.getMessage(), ex);

    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "サーバーエラーが発生しました"));
  }
}
