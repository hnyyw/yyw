package com.example.netdisk.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BizException.class)
  public ResponseEntity<ApiResponse<Object>> handleBiz(BizException e) {
    HttpStatus status =
        switch (e.getErrorCode()) {
          case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
          case FORBIDDEN -> HttpStatus.FORBIDDEN;
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case CONFLICT, UPLOAD_CONFLICT -> HttpStatus.CONFLICT;
          case TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
          case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
          default -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  public ResponseEntity<ApiResponse<Object>> handleValidation(Exception e) {
    return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, "参数错误"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception e) {
    return ResponseEntity.status(500).body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "服务器错误"));
  }
}
