package com.rookies6.udt.controller;

// TODO(T-006) BE-C — 하드코딩 껍데기(SPEC.md §4.2·§4.3 예시 그대로).
// ProductService 연결 시 이 클래스의 본문만 바꾼다. URL·응답 형태는 계약이므로 그대로 둔다.

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.ProductDetailResponse;
import com.rookies6.udt.dto.ProductImageResponse;
import com.rookies6.udt.dto.ProductSummaryResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        if (page < 0 || size < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        int pageSize = Math.min(size, 100);
        List<ProductSummaryResponse> content = "__none__".equals(q) ? List.of() : List.of(sample());
        PageImpl<ProductSummaryResponse> result =
                new PageImpl<>(content, PageRequest.of(page, pageSize), content.size());

        return ApiResponse.of(PageResponse.from(result), "상품 목록 조회가 완료되었습니다");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> detail(@PathVariable Long id) {
        if (id == 999999999L) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductDetailResponse detail = new ProductDetailResponse(
                String.valueOf(id), "맥북 에어 M2 13인치", "2023년 구매, 배터리 사이클 120회",
                850000L, "A", "ON_SALE", "노트북", "2", "판매왕",
                List.of(new ProductImageResponse("31", "/api/products/" + id + "/images/31", 0)),
                false, 3, OffsetDateTime.now(), OffsetDateTime.now());
        return ApiResponse.of(detail, "상품 상세 조회가 완료되었습니다");
    }

    private ProductSummaryResponse sample() {
        return new ProductSummaryResponse("12", "맥북 에어 M2 13인치", 850000L, "A", "ON_SALE",
                "노트북", "판매왕", "/api/products/12/images/31", 3, OffsetDateTime.now());
    }
}
