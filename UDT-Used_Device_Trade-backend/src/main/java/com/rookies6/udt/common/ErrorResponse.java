package com.rookies6.udt.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(boolean success, int statusCode, String code, String message,
                            List<FieldError> fields, OffsetDateTime timestamp) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getStatus().value(), errorCode.name(),
                message, null, OffsetDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> fields) {
        return new ErrorResponse(false, errorCode.getStatus().value(), errorCode.name(),
                message, fields, OffsetDateTime.now());
    }

    public record FieldError(String name, String message) {
    }
}
