package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);
    int countByProductId(Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
