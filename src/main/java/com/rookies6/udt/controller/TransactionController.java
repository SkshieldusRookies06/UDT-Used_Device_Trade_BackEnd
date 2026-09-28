package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.dto.ShippingRequest;
import com.rookies6.udt.dto.TransactionResponse;
import com.rookies6.udt.security.CurrentUser;
import com.rookies6.udt.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 담당 범위: 구매 · 송장 · 구매확정 · 거래 상세
// 관리자 처리는 T-018 Thymeleaf 컨트롤러가 TransactionService를 직접 호출한다.
@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // SPEC 4.5 POST /api/products/{id}/purchase → 201
    @PostMapping("/api/products/{productId}/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> purchase(@PathVariable Long productId) {
        return ApiResponse.of(transactionService.purchase(CurrentUser.id(), productId),
                "구매가 완료되었습니다");
    }

    // SPEC 4.9 GET /api/transactions/{id} → 200
    @GetMapping("/api/transactions/{transactionId}")
    public ApiResponse<TransactionResponse> getDetail(@PathVariable Long transactionId) {
        return ApiResponse.of(transactionService.getDetail(CurrentUser.id(), transactionId),
                "거래 조회가 완료되었습니다");
    }

    // SPEC 4.7 PATCH /api/transactions/{id}/shipping → 200
    @PatchMapping("/api/transactions/{transactionId}/shipping")
    public ApiResponse<TransactionResponse> registerShipping(@PathVariable Long transactionId,
                                                             @Valid @RequestBody ShippingRequest request) {
        return ApiResponse.of(transactionService.registerShipping(CurrentUser.id(), transactionId, request),
                "송장 입력이 완료되었습니다");
    }

    // SPEC 4.6 PATCH /api/transactions/{id}/confirm → 200
    @PatchMapping("/api/transactions/{transactionId}/confirm")
    public ApiResponse<TransactionResponse> confirm(@PathVariable Long transactionId) {
        return ApiResponse.of(transactionService.confirm(CurrentUser.id(), transactionId),
                "구매 확정이 완료되었습니다");
    }
}