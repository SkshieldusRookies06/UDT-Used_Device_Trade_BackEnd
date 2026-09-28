package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 환불 후 재구매 → 상품당 거래 다건. Optional이면 2건에서 예외가 난다
    List<Transaction> findByProductId(Long productId);
}