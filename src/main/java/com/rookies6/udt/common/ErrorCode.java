package com.rookies6.udt.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다"),
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "판매 중인 상품이 아닙니다"),
    SELF_PURCHASE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인이 등록한 상품은 구매할 수 없습니다"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "잔액이 부족합니다"),

    WISH_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 찜한 상품입니다"),
    WISH_NOT_FOUND(HttpStatus.NOT_FOUND, "찜하지 않은 상품입니다"),

    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다"),
    TRANSACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 거래의 당사자가 아닙니다"),
    INVALID_TRANSACTION_STATUS(HttpStatus.CONFLICT, "현재 거래 상태에서는 처리할 수 없습니다"),

    DISPUTE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 분쟁이 접수된 거래입니다"),
    DISPUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "분쟁을 찾을 수 없습니다"),

    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다"),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "첨부 가능한 파일 수를 초과했습니다"),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하지 못했습니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
