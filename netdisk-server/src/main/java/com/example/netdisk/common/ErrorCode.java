package com.example.netdisk.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
  BAD_REQUEST(40001),
  UNAUTHORIZED(40100),
  FORBIDDEN(40300),
  NOT_FOUND(40400),
  CONFLICT(40900),
  UPLOAD_CONFLICT(40910),
  TOO_LARGE(41300),
  RATE_LIMIT(42900),
  INTERNAL_ERROR(50000);

  private final int code;

  ErrorCode(int code) {
    this.code = code;
  }
}
