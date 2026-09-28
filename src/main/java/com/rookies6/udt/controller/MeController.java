package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.ProductSummaryResponse;
import com.rookies6.udt.dto.TransactionResponse;
import com.rookies6.udt.security.CurrentUser;
import com.rookies6.udt.service.MeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSummaryResponse>> myProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ApiResponse.of(meService.myProducts(CurrentUser.id(), page, size),
                "내 상품 목록 조회가 완료되었습니다");
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<TransactionResponse>> myTransactions(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ApiResponse.of(meService.myTransactions(CurrentUser.id(), role, page, size),
                "내 거래 목록 조회가 완료되었습니다");
    }

    @GetMapping("/wishes")
    public ApiResponse<PageResponse<ProductSummaryResponse>> myWishes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ApiResponse.of(meService.myWishes(CurrentUser.id(), page, size),
                "내 찜 목록 조회가 완료되었습니다");
    }
}
