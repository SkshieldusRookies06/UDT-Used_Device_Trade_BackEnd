package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.WishResponse;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.entity.Wish;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.UserRepository;
import com.rookies6.udt.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishService {

    private final WishRepository wishRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishResponse addWish(Long userId, Long productId) {

        if (wishRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(ErrorCode.WISH_ALREADY_EXISTS);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Wish wish = Wish.builder()
                .user(user)
                .product(product)
                .build();

        wishRepository.save(wish);

        int wishCount = wishRepository.countByProductId(productId);

        return new WishResponse(String.valueOf(productId), true, wishCount);
    }

    public WishResponse removeWish(Long userId, Long productId) {

        if (!wishRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(ErrorCode.WISH_NOT_FOUND);
        }

        wishRepository.deleteByUserIdAndProductId(userId, productId);

        int wishCount = wishRepository.countByProductId(productId);

        return new WishResponse(String.valueOf(productId), false, wishCount);
    }

}
