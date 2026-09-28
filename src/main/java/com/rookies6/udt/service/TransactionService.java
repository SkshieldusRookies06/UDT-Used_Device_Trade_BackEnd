package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.ShippingRequest;
import com.rookies6.udt.dto.TransactionResponse;
import com.rookies6.udt.entity.*;
import com.rookies6.udt.repository.DisputeRepository;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.TransactionRepository;
import com.rookies6.udt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;   // BE-C 소유
    private final UserRepository userRepository;         // BE-A 소유
    private final DisputeRepository disputeRepository;   // BE-A 소유

    // ── BR-01 관리자 검수 승인 ──────────────────────────────
    @Transactional
    public void approveInspection(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.INSPECTING) {
            // TODO(BE-A 요청): 상품 전용 에러코드 검토 (지금은 임시 대체)
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        product.changeStatus(ProductStatus.ON_SALE);
    }

    // ── BR-02 관리자 검수 반려 ──────────────────────────────
    @Transactional
    public void rejectInspection(Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.INSPECTING) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        product.reject(reason);
    }

    // ── BR-03 구매 요청 ──────────────────────────────────────
    @Transactional
    public TransactionResponse purchase(Long buyerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        if (product.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.SELF_PURCHASE_NOT_ALLOWED);
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (buyer.getBalanceKrw() < product.getPriceKrw()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // ── 여기부터 원자적 ──
        int updated = productRepository.transition(
                productId, ProductStatus.ON_SALE, ProductStatus.IN_TRADE);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);   // 동시에 누가 먼저 샀다
        }
        // 벌크 UPDATE는 메모리 객체를 안 바꾼다. 맞춰 두지 않으면 다른 flush 때 ON_SALE을 되쓴다
        product.changeStatus(ProductStatus.IN_TRADE);

        buyer.withdraw(product.getPriceKrw());   // 판매자 잔액은 건드리지 않는다 (에스크로)

        Transaction transaction = Transaction.builder()
                .product(product)
                .buyer(buyer)
                .amountKrw(product.getPriceKrw())
                .build();   // status = PAID

        transactionRepository.save(transaction);
        return TransactionResponse.from(transaction);
    }

    // ── 거래 상세 (SPEC 4.9) — 당사자(구매자·판매자)만 ──────────
    @Transactional(readOnly = true)
    public TransactionResponse getDetail(Long requesterId, Long transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        boolean isBuyer = transaction.getBuyer().getId().equals(requesterId);
        boolean isSeller = transaction.getProduct().getSeller().getId().equals(requesterId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.TRANSACTION_FORBIDDEN);
        }

        Dispute dispute = disputeRepository.findByTransactionId(transactionId).orElse(null);
        return TransactionResponse.from(transaction, dispute);
    }

    // ── BR-04 송장 입력 ──────────────────────────────────────
    @Transactional
    public TransactionResponse registerShipping(Long sellerId, Long transactionId, ShippingRequest request) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (!transaction.getProduct().getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.TRANSACTION_FORBIDDEN);
        }
        if (transaction.getStatus() != TransactionStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

        transaction.registerShipping(request.courier(), request.trackingNo());
        return TransactionResponse.from(transaction);
    }

    // ── BR-05 구매 확정 ──────────────────────────────────────
    @Transactional
    public TransactionResponse confirm(Long buyerId, Long transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.TRANSACTION_FORBIDDEN);
        }
        if (transaction.getStatus() != TransactionStatus.SHIPPING) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

        transaction.confirm(OffsetDateTime.now());
        transaction.getProduct().changeStatus(ProductStatus.SOLD);
        transaction.getProduct().getSeller().deposit(transaction.getAmountKrw());

        return TransactionResponse.from(transaction);
    }

    // ── BR-06 분쟁 신고 — 전이만. Dispute 생성은 BE-A(T-010)가 이 메서드를 호출 ──
    @Transactional
    public void markDisputed(Long buyerId, Long transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.TRANSACTION_FORBIDDEN);
        }
        if (transaction.getStatus() != TransactionStatus.PAID
                && transaction.getStatus() != TransactionStatus.SHIPPING) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

        transaction.changeStatus(TransactionStatus.DISPUTED);
    }

    // ── BR-07 관리자 강제 환불 (T-018 관리자 화면이 호출) ──────────
    @Transactional
    public TransactionResponse forceRefund(Long transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (transaction.getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

        transaction.changeStatus(TransactionStatus.REFUNDED);
        transaction.getProduct().changeStatus(ProductStatus.ON_SALE);
        transaction.getBuyer().deposit(transaction.getAmountKrw());
        Dispute dispute = resolveDispute(transaction, "관리자 강제 환불");

        return TransactionResponse.from(transaction, dispute);
    }

    // ── BR-08 관리자 강제 확정 (T-018 관리자 화면이 호출) ──────────
    @Transactional
    public TransactionResponse forceConfirm(Long transactionId) {
        Transaction transaction = getTransactionOrThrow(transactionId);

        if (transaction.getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

        transaction.changeStatus(TransactionStatus.CONFIRMED);
        transaction.getProduct().changeStatus(ProductStatus.SOLD);
        transaction.getProduct().getSeller().deposit(transaction.getAmountKrw());
        Dispute dispute = resolveDispute(transaction, "관리자 강제 확정");

        return TransactionResponse.from(transaction, dispute);
    }

    // 분쟁이 있고 아직 RESOLVED가 아니면 종결한다. BR-07·08 네 번째 줄 (같은 트랜잭션)
    private Dispute resolveDispute(Transaction transaction, String memo) {
        Dispute dispute = disputeRepository.findByTransactionId(transaction.getId()).orElse(null);
        if (dispute != null && dispute.getStatus() != DisputeStatus.RESOLVED) {
            dispute.resolve(memo, OffsetDateTime.now());
        }
        return dispute;
    }

    private Transaction getTransactionOrThrow(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }
}