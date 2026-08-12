package com.example.springlab.error;

import java.time.OffsetDateTime;

public record ErrorResponse(OffsetDateTime timestamp, String code, String message) {

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(OffsetDateTime.now(), code, message);
  }
}
