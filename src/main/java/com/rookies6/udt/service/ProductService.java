package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.*;
import com.rookies6.udt.entity.*;
import com.rookies6.udt.repository.CategoryRepository;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.UserRepository;
import com.rookies6.udt.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final WishRepository wishRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final FileStorageService fileStorageService;

    public List<ProductSummaryResponse> findInspectingProducts() {

        List<Product> products = productRepository.findProductsWithOptions(
                ProductStatus.INSPECTING, null, null, Pageable.unpaged()
        ).getContent();

        Map<Long, Integer> wishCounts = getWishCounts(products);

        return products.stream()
                .map(product -> toSummaryResponse(product,
                        wishCounts.getOrDefault(product.getId(), 0)))
                .toList();
    }

    public PageResponse<ProductSummaryResponse> getProducts(
            String q, Long categoryId, int page, int size) {

        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> pages = productRepository.findProductsWithOptions(
                ProductStatus.ON_SALE,
                normalizedQ, categoryId, pageable);

        Map<Long, Integer> wishCounts = getWishCounts(pages.getContent());

        Page<ProductSummaryResponse> result = pages.map(
                product -> toSummaryResponse(product, wishCounts.getOrDefault(product.getId(), 0))
        );
        return PageResponse.from(result);
    }

    private Map<Long, Integer> getWishCounts(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();

        return wishRepository.findWishCountsByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    public ProductDetailResponse getDetail(Long id, Long viewerId) {

        Product product = productRepository.findById(id)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
                );

        return toDetailResponse(product, viewerId);
    }

    @Transactional
    public ProductDetailResponse create(Long sellerId, ProductCreateRequest request, List<MultipartFile> images) {

        List<MultipartFile> files =
                images == null ? List.of() : images.stream()
                        .filter(f -> !f.isEmpty()).toList();

        if (files.size() > 5) {
            throw new BusinessException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Long categoryId = Long.parseLong(request.categoryId());

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));

        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .priceKrw(request.priceKrw())
                .conditionGrade(ConditionGrade.valueOf(request.conditionGrade()))
                .build();

        List<String> storedNames = new ArrayList<>();

        try {
            int sortOrder = 0;
            for (MultipartFile file : files) {
                StoredFile stored = fileStorageService.store(file, FileStorageService.PRODUCTS);
                storedNames.add(stored.storedName());
                product.addImage(new ProductImage(stored.storedName(), stored.originalName(), sortOrder++));
            }

            productRepository.save(product);

        } catch (RuntimeException e) {
            storedNames.forEach(name ->
                    fileStorageService.delete(FileStorageService.PRODUCTS, name));
            throw e;
        }

        return toDetailResponse(product, sellerId);
    }


    private ProductDetailResponse toDetailResponse(Product product, Long viewerId) {

        String baseUrl = "/api/products/" + product.getId() + "/images/";

        List<ProductImageResponse> images =
                product.getImages().stream().map(
                        image -> new ProductImageResponse(
                                String.valueOf(image.getId()),
                                baseUrl + image.getId(),
                                image.getSortOrder()
                        )).toList();

        boolean wished = viewerId != null
                &&
                wishRepository.existsByUserIdAndProductId(viewerId, product.getId());

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
                wished,
                wishRepository.countByProductId(product.getId()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private ProductSummaryResponse toSummaryResponse(Product product, int wishCount) {

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
                wishCount,
                product.getCreatedAt()
        );
    }
}
