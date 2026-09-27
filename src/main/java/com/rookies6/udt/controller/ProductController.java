package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.ProductCreateRequest;
import com.rookies6.udt.dto.ProductDetailResponse;
import com.rookies6.udt.dto.ProductSummaryResponse;

import com.rookies6.udt.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDetailResponse> create(
            @RequestPart("product") @Valid ProductCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
            ) {

        // TODO(T-005) — 로그인 사용자 id 추출
        Long sellerId = null;

        ProductDetailResponse response = productService.create(sellerId, request, images);
        return ApiResponse.of(response, "상품 등록이 완료되었습니다");
    }

}
