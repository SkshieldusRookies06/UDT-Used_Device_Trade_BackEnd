package com.rookies6.udt.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(boolean success, T data, String message, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(true, data, message, OffsetDateTime.now());
    }
}
