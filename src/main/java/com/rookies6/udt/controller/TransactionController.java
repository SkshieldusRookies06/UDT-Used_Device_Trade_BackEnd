package com.rookies6.udt.controller;

import com.rookies6.udt.entity.Transaction;
import com.rookies6.udt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // BR-03 구매 요청
    // TODO: buyerId는 임시로 파라미터로 받음. BE-B 인증 붙으면 로그인 사용자에서 가져오도록 교체
    @PostMapping("/api/products/{productId}/purchase")
    public Transaction purchase(@RequestParam Long buyerId, @PathVariable Long productId) {
        return transactionService.purchase(buyerId, productId);
    }

    // BR-04 송장 입력
    @PostMapping("/api/transactions/{transactionId}/shipping")
    public Transaction registerShipping(@RequestParam Long sellerId,
                                        @PathVariable Long transactionId,
                                        @RequestParam String courier,
                                        @RequestParam String trackingNo) {
        return transactionService.registerShipping(sellerId, transactionId, courier, trackingNo);
    }

    // BR-05 구매 확정
    @PostMapping("/api/transactions/{transactionId}/confirm")
    public Transaction confirm(@RequestParam Long buyerId, @PathVariable Long transactionId) {
        return transactionService.confirm(buyerId, transactionId);
    }

    // BR-07 관리자 강제 환불
    @PostMapping("/api/admin/transactions/{transactionId}/force-refund")
    public Transaction forceRefund(@PathVariable Long transactionId) {
        return transactionService.forceRefund(transactionId);
    }

    // BR-08 관리자 강제 확정
    @PostMapping("/api/admin/transactions/{transactionId}/force-confirm")
    public Transaction forceConfirm(@PathVariable Long transactionId) {
        return transactionService.forceConfirm(transactionId);
    }
}