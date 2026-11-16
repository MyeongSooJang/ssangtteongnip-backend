package com.ssangtteongnip.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ssangtteongnip.global.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String code, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode.name(), errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, code, message);
    }
}
