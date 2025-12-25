package com.example.netdisk.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  private int code;
  private String message;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private T data;
  private String requestId;

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "OK", data, RequestId.get());
  }

  public static ApiResponse<Object> ok() {
    return new ApiResponse<>(0, "OK", null, RequestId.get());
  }

  public static ApiResponse<Object> error(ErrorCode errorCode, String message) {
    return new ApiResponse<>(errorCode.getCode(), message, null, RequestId.get());
  }
}
