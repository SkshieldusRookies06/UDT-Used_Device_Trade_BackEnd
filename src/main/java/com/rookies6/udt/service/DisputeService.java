package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.DisputeCreateRequest;
import com.rookies6.udt.dto.DisputeResponse;
import com.rookies6.udt.dto.StoredFile;
import com.rookies6.udt.entity.Dispute;
import com.rookies6.udt.entity.DisputeFile;
import com.rookies6.udt.entity.DisputeStatus;
import com.rookies6.udt.entity.Transaction;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.DisputeFileRepository;
import com.rookies6.udt.repository.DisputeRepository;
import com.rookies6.udt.repository.TransactionRepository;
import com.rookies6.udt.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisputeService {

    private static final int MAX_FILES = 3;

    private final DisputeRepository disputeRepository;
    private final DisputeFileRepository disputeFileRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final FileStorageService fileStorageService;

    /**
     * 분쟁 접수(BR-06). 거래 상태 전이는 {@code TransactionService.markDisputed}가 하고(SPEC §2.5),
     * 이 메서드와 한 트랜잭션이라 둘 중 하나가 실패하면 함께 롤백된다.
     */
    @Transactional
    public DisputeResponse open(Long buyerId, Long transactionId,
                                DisputeCreateRequest request, List<MultipartFile> files) {

        List<MultipartFile> attachments = files == null ? List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (attachments.size() > MAX_FILES) {
            throw new BusinessException(ErrorCode.FILE_COUNT_EXCEEDED);
        }
        if (disputeRepository.existsByTransactionId(transactionId)) {
            throw new BusinessException(ErrorCode.DISPUTE_ALREADY_EXISTS);
        }

        transactionService.markDisputed(buyerId, transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
        User reporter = userRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Dispute dispute = Dispute.builder()
                .transaction(transaction)
                .reporter(reporter)
                .reason(request.reason())
                .build();

        List<String> storedNames = new ArrayList<>();
        try {
            for (MultipartFile file : attachments) {
                StoredFile stored = fileStorageService.store(file, FileStorageService.DISPUTES);
                storedNames.add(stored.storedName());
                dispute.addFile(DisputeFile.builder()
                        .storedName(stored.storedName())
                        .originalName(stored.originalName())
                        .sizeBytes(stored.sizeBytes())
                        .build());
            }
            disputeRepository.save(dispute);

        } catch (RuntimeException e) {
            storedNames.forEach(name -> fileStorageService.delete(FileStorageService.DISPUTES, name));
            throw e;
        }

        return DisputeResponse.from(dispute);
    }

    /** 관리자 화면(T-018 · BE-B)이 호출하는 미해결 분쟁 목록. */
    public List<DisputeResponse> findOpenDisputes() {
        return disputeRepository.findByStatusOrderByCreatedAtDesc(DisputeStatus.OPEN).stream()
                .map(DisputeResponse::from)
                .toList();
    }

    /** React 경로 — 거래 당사자(구매자·판매자)만 내려받는다. */
    public FileContent downloadForParty(Long disputeId, Long fileId, Long requesterId) {
        Dispute dispute = getDisputeOrThrow(disputeId);
        Transaction transaction = dispute.getTransaction();

        boolean party = transaction.getBuyer().getId().equals(requesterId)
                || transaction.getProduct().getSeller().getId().equals(requesterId);
        if (!party) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return read(disputeId, fileId);
    }

    /** 관리자 화면 경로 — Security 체인 2가 ADMIN을 보장하므로 당사자 검사를 하지 않는다(SPEC §7). */
    public FileContent downloadForAdmin(Long disputeId, Long fileId) {
        getDisputeOrThrow(disputeId);
        return read(disputeId, fileId);
    }

    private FileContent read(Long disputeId, Long fileId) {
        DisputeFile file = disputeFileRepository.findByIdAndDisputeId(fileId, disputeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPUTE_NOT_FOUND));

        Resource resource = fileStorageService.load(FileStorageService.DISPUTES, file.getStoredName());
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("증빙 행은 있는데 파일이 없다 fileId={} storedName={}", fileId, file.getStoredName());
            throw new BusinessException(ErrorCode.DISPUTE_NOT_FOUND);
        }
        return new FileContent(resource, fileStorageService.contentType(file.getStoredName()),
                file.getOriginalName());
    }

    private Dispute getDisputeOrThrow(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISPUTE_NOT_FOUND));
    }

    public record FileContent(Resource resource, String contentType, String originalName) {

        /** 한글 파일명도 깨지지 않게 RFC 5987(filename*=UTF-8'')로 내보낸다. */
        public String contentDisposition() {
            return ContentDisposition.attachment()
                    .filename(originalName, StandardCharsets.UTF_8)
                    .build()
                    .toString();
        }
    }
}
