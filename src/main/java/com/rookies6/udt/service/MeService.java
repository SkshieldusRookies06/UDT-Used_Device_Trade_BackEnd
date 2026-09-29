package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.common.PageResponse;
import com.rookies6.udt.dto.ProductSummaryResponse;
import com.rookies6.udt.dto.TransactionResponse;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.Transaction;
import com.rookies6.udt.repository.MeQueryRepository;
import com.rookies6.udt.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeService {

    private static final int MAX_SIZE = 100;

    private final MeQueryRepository meQueryRepository;
    private final WishRepository wishRepository;

    public PageResponse<ProductSummaryResponse> myProducts(Long userId, int page, int size) {
        Page<Product> found = meQueryRepository.findMyProducts(userId, pageable(page, size));
        return PageResponse.from(toSummaryPage(found));
    }

    public PageResponse<ProductSummaryResponse> myWishes(Long userId, int page, int size) {
        Page<Product> found = meQueryRepository.findMyWishedProducts(userId, pageable(page, size));
        return PageResponse.from(toSummaryPage(found));
    }

    public PageResponse<TransactionResponse> myTransactions(Long userId, String role, int page, int size) {
        Pageable pageable = pageable(page, size);
        Page<Transaction> found = switch (role == null ? "" : role) {
            case "buyer" -> meQueryRepository.findMyTransactionsAsBuyer(userId, pageable);
            case "seller" -> meQueryRepository.findMyTransactionsAsSeller(userId, pageable);
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
        return PageResponse.from(found.map(TransactionResponse::from));
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return PageRequest.of(page, Math.min(size, MAX_SIZE));
    }

    /** 찜 수는 페이지 단위로 한 번에 센다 — 행마다 세면 N+1이다(T-019와 같은 집계 메서드를 쓴다). */
    private Page<ProductSummaryResponse> toSummaryPage(Page<Product> products) {
        Map<Long, Integer> wishCounts = wishCounts(products.getContent());
        return products.map(product -> toSummary(product, wishCounts.getOrDefault(product.getId(), 0)));
    }

    private Map<Long, Integer> wishCounts(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        return wishRepository.findWishCountsByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()));
    }

    private ProductSummaryResponse toSummary(Product product, int wishCount) {
        String thumbnailUrl = product.getImages().isEmpty()
                ? null
                : "/api/products/" + product.getId() + "/images/" + product.getImages().get(0).getId();

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
                product.getCreatedAt());
    }
}
