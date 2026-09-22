package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.ProductDetailResponse;
import com.rookies6.udt.dto.ProductImageResponse;
import com.rookies6.udt.dto.ProductSummaryResponse;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final WishRepository wishRepository;

    public PageResponse<ProductSummaryResponse> getProducts(
            String q, Long categoryId, int page, int size) {

        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> pages = productRepository.findProductsWithOptions(
                ProductStatus.ON_SALE,
                normalizedQ, categoryId, pageable);

        Page<ProductSummaryResponse> result = pages.map(this::toSummaryResponse);
        return PageResponse.from(result);
    }

    public ProductDetailResponse getDetail(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        return toDetailResponse(product);
    }

    private ProductDetailResponse toDetailResponse(Product product) {

        String baseUrl = "/api/products/" + product.getId() + "/images/";

        List<ProductImageResponse> images =
                product.getImages().stream().map(
                        image -> new ProductImageResponse(
                                String.valueOf(image.getId()),
                                baseUrl + image.getId(),
                                image.getSortOrder()
                        )).toList();

        return new ProductDetailResponse(
                String.valueOf(product.getId()),
                product.getTitle(),
                product.getDescription(),
                product.getPriceKrw(),
                product.getConditionGrade().name(),
                product.getStatus().name(),
                product.getCategory().getName(),
                String.valueOf(product.getSeller().getId()),
                product.getSeller().getNickname(),
                images,
                false,
                wishRepository.countByProductId(product.getId()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {

        String thumbnailUrl = product.getImages().isEmpty()
                ? null
                : "/api/products/" + product.getId() + "/images/"
                + product.getImages().get(0).getId();

        return new ProductSummaryResponse(
                String.valueOf(product.getId()),
                product.getTitle(),
                product.getPriceKrw(),
                product.getConditionGrade().name(),
                product.getStatus().name(),
                product.getCategory().getName(),
                product.getSeller().getNickname(),
                thumbnailUrl,
                wishRepository.countByProductId(product.getId()),
                product.getCreatedAt()
        );
    }
}
