package com.rookies6.udt.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static boolean isAdmin(HttpServletRequest req) {
        return req.getRequestURI().startsWith("/admin");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                        ErrorCode.VALIDATION_ERROR.getMessage(), fields));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                        ErrorCode.VALIDATION_ERROR.getMessage()));
    }

    // 상한을 넘는 파일은 multipart 파싱에서 끊겨 FileStorageService까지 오지 않는다 (T-023)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.getStatus())
                .body(ErrorResponse.of(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    // 계약 밖 요청도 공통 봉투로 돌려준다 — 여기가 없으면 아래 Exception 핸들러가 500으로 감싸 원인이 가려진다
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest req) throws Exception {
        if (isAdmin(req)) throw e;
        log.warn("허용되지 않는 메서드 {} {}", req.getMethod(), req.getRequestURI());
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e,
                                                                    HttpServletRequest req) throws Exception {
        if (isAdmin(req)) throw e;
        log.warn("지원하지 않는 Content-Type {} {} ({})", req.getMethod(), req.getRequestURI(), req.getContentType());
        return ResponseEntity.status(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
                .body(ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e, HttpServletRequest req)
            throws NoResourceFoundException {
        if (isAdmin(req)) throw e;
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest req) {
        if (isAdmin(req)) throw e;
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest req) throws Exception {
        log.error("unhandled {} {}", req.getMethod(), req.getRequestURI(), e);
        if (isAdmin(req)) throw e;
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
