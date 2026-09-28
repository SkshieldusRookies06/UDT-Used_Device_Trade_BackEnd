package com.rookies6.udt.repository;

import com.rookies6.udt.entity.DisputeFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeFileRepository extends JpaRepository<DisputeFile, Long> {

    Optional<DisputeFile> findByIdAndDisputeId(Long id, Long disputeId);
}
