package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Dispute;
import com.rookies6.udt.entity.DisputeStatus;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import com.rookies6.udt.entity.TransactionStatus;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.DisputeRepository;
import com.rookies6.udt.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 관리자 강제 환불·확정(BR-07·08) — BE-D 0절 ③.
 * SPEC §2.5 표의 네 번째 줄 "Dispute → RESOLVED"가 거래·상품·잔액과 같은 트랜잭션에서 일어나야 한다.
 * 빠지면 "환불됐는데 관리자 분쟁 목록엔 OPEN"이 된다.
 */
@AcceptanceTest
@DisplayName("[BE-D ③] 강제 환불·확정 — 분쟁도 RESOLVED로 닫힌다")
class ForceResolutionDisputeTest extends AcceptanceSupport {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private DisputeRepository disputeRepository;

    private User seller;
    private User buyer;
    private Product product;
    private Long txId;

    @BeforeEach
    void setUp() {
        seller = member("fr-seller", 0L);
        buyer = member("fr-buyer", 1_000_000L);
        product = onSaleProduct(seller, 300_000L);
        flushAndClear();
        txId = paidTransaction(buyer, product);
    }

    /** T-010(BE-A)이 할 일을 흉내 낸다 — 거래를 DISPUTED로 바꾸고 분쟁 행을 만든다. */
    private void openDispute() {
        transactionService.markDisputed(buyer.getId(), txId);
        disputeRepository.saveAndFlush(Dispute.builder()
                .transaction(transactionRepository.findById(txId).orElseThrow())
                .reporter(userRepository.findById(buyer.getId()).orElseThrow())
                .reason("액정에 멍이 있습니다 — 테스트").build());
        flushAndClear();
    }

    @Test
    @DisplayName("③ 강제 환불 — 거래 REFUNDED · 상품 ON_SALE · 구매자 잔액 복구 · 분쟁 RESOLVED + resolvedAt")
    void 강제_환불하면_분쟁이_닫힌다() {
        openDispute();

        transactionService.forceRefund(txId);
        flushAndClear();

        assertThat(transactionRepository.findById(txId).orElseThrow().getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
        Dispute dispute = disputeRepository.findByTransactionId(txId).orElseThrow();
        assertThat(dispute.getStatus()).as("분쟁 상태 — OPEN이면 resolveDispute가 빠진 것").isEqualTo(DisputeStatus.RESOLVED);
        assertThat(dispute.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("③ 강제 확정 — 거래 CONFIRMED · 상품 SOLD · 판매자 입금 · 분쟁 RESOLVED + resolvedAt")
    void 강제_확정하면_분쟁이_닫힌다() {
        openDispute();

        transactionService.forceConfirm(txId);
        flushAndClear();

        assertThat(transactionRepository.findById(txId).orElseThrow().getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(userRepository.findById(seller.getId()).orElseThrow().getBalanceKrw()).isEqualTo(300_000L);
        Dispute dispute = disputeRepository.findByTransactionId(txId).orElseThrow();
        assertThat(dispute.getStatus()).as("분쟁 상태 — OPEN이면 resolveDispute가 빠진 것").isEqualTo(DisputeStatus.RESOLVED);
        assertThat(dispute.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("(지킴) 분쟁 행 없이 DISPUTED만 된 거래도 강제 환불은 된다 — 분쟁이 없으면 그냥 지나간다")
    void 분쟁_행이_없어도_강제_환불은_된다() {
        transactionService.markDisputed(buyer.getId(), txId);
        flushAndClear();

        transactionService.forceRefund(txId);
        flushAndClear();

        assertThat(transactionRepository.findById(txId).orElseThrow().getStatus()).isEqualTo(TransactionStatus.REFUNDED);
    }
}
