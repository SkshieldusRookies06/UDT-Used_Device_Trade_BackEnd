package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * `/api/me/*` 전용 조회(T-021 · 오너 BE-A). 상품·찜·거래에 걸쳐 있어 도메인 Repository(BE-C·BE-D)에
 * 메서드를 끼워 넣지 않고 여기 모았다.
 *
 * <p>fetch join은 seller·category처럼 <b>단건 연관만</b> 한다. 이미지(컬렉션)는 페이징과 함께
 * fetch join하면 메모리 페이징이 되므로 건드리지 않는다 — N+1은 T-019(BE-C)가 batch fetch로 잡는다.
 */
public interface MeQueryRepository extends Repository<Product, Long> {

    @Query(value = "select p from Product p join fetch p.seller join fetch p.category "
            + "where p.seller.id = :sellerId order by p.createdAt desc",
            countQuery = "select count(p) from Product p where p.seller.id = :sellerId")
    Page<Product> findMyProducts(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query(value = "select p from Wish w join w.product p join fetch p.seller join fetch p.category "
            + "where w.user.id = :userId order by w.createdAt desc",
            countQuery = "select count(w) from Wish w where w.user.id = :userId")
    Page<Product> findMyWishedProducts(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "select t from Transaction t join fetch t.product p join fetch p.seller "
            + "join fetch t.buyer where t.buyer.id = :buyerId order by t.createdAt desc",
            countQuery = "select count(t) from Transaction t where t.buyer.id = :buyerId")
    Page<Transaction> findMyTransactionsAsBuyer(@Param("buyerId") Long buyerId, Pageable pageable);

    @Query(value = "select t from Transaction t join fetch t.product p join fetch p.seller "
            + "join fetch t.buyer where p.seller.id = :sellerId order by t.createdAt desc",
            countQuery = "select count(t) from Transaction t where t.product.seller.id = :sellerId")
    Page<Transaction> findMyTransactionsAsSeller(@Param("sellerId") Long sellerId, Pageable pageable);
}
