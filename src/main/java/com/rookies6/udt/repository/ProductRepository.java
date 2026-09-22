package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(
            value = "SELECT p FROM Product p " +
                    "JOIN FETCH p.seller " +
                    "JOIN FETCH p.category " +
                    "WHERE p.status = :status " +
                    "AND (:q IS NULL OR p.title LIKE CONCAT('%', :q, '%')) " +
                    "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
                    "ORDER BY p.createdAt DESC",

            countQuery = "SELECT COUNT(p) FROM Product p " +
                    "WHERE p.status = :status " +
                    "AND (:q IS NULL OR p.title LIKE CONCAT('%', :q, '%')) " +
                    "AND (:categoryId IS NULL OR p.category.id = :categoryId)"
    )
    Page<Product> findProductsWithOptions(
            @Param("status") ProductStatus status,
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.status = :next where p.id = :id and p.status = :expected")
    int transition(@Param("id") Long id,
                   @Param("expected") ProductStatus expected,
                   @Param("next") ProductStatus next);
}
