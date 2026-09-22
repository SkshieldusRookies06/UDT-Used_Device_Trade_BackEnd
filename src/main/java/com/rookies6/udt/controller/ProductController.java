package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.ProductDetailResponse;
import com.rookies6.udt.dto.ProductSummaryResponse;

import com.rookies6.udt.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> list(

            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        if (page < 0 || size < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        int pageSize = Math.min(size, 100);

        PageResponse<ProductSummaryResponse> result =
                productService.getProducts(q, categoryId, page, pageSize);

        return ApiResponse.of(result, "상품 목록 조회가 완료되었습니다");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long id) {

        ProductDetailResponse detailResponse = productService.getDetail(id);
        return ApiResponse.of(detailResponse, "상품 상세 조회가 완료되었습니다");
    }

}
