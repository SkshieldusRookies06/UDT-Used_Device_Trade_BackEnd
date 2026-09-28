package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishRepository extends JpaRepository<Wish, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);
    int countByProductId(Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);

    @Query(
            "select w.product.id, count(w) from Wish w " +
            "where w.product.id in :productIds " +
            "group by w.product.id"
    )
    List<Object[]> findWishCountsByProductIds(@Param("productIds") List<Long> productIds);
}
