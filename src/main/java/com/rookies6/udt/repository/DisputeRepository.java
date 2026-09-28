package com.rookies6.udt.repository;

import com.rookies6.udt.entity.Dispute;
import com.rookies6.udt.entity.DisputeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    boolean existsByTransactionId(Long transactionId);

    Optional<Dispute> findByTransactionId(Long transactionId);

    /** 관리자 화면(T-018)의 분쟁 목록. 화면이 증빙 파일까지 그리므로 files를 함께 읽는다(페이징 없음). */
    @EntityGraph(attributePaths = {"transaction", "files"})
    List<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status);
}
