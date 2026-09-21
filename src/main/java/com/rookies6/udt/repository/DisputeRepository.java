package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    boolean existsByTransactionId(Long transactionId);
}
