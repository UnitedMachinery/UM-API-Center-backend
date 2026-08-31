package com.um.apicenter.common;

import java.util.Map;

public record ApiResponse<T>(String request_id, T data, Map<String, Object> meta, ApiError error) {

    public static <T> ApiResponse<T> success(String requestId, T data, Map<String, Object> meta) {
        return new ApiResponse<>(requestId, data, meta, null);
    }

    public static <T> ApiResponse<T> failure(String requestId, String code, String message) {
        return new ApiResponse<>(requestId, null, null, new ApiError(code, message));
    }
}
