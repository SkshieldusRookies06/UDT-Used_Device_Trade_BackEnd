package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import com.rookies6.udt.entity.TransactionStatus;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 구매(BR-03)의 경계 — T-022 수용 기준.
 * 잔액 비교가 {@code <}가 아니라 {@code <=}로 바뀌면 딱 맞는 잔액으로 못 산다.
 */
@AcceptanceTest
@DisplayName("[BE-A · T-022] 구매 경계 — 잔액이 딱 맞는 구매 · 한 사람의 연속 구매")
class PurchaseEdgeCaseTest extends AcceptanceSupport {

    @Autowired private TransactionRepository transactionRepository;

    @Test
    @DisplayName("잔액과 가격이 같으면 살 수 있고 잔액은 정확히 0이 된다")
    void 잔액이_딱_맞으면_0이_남는다() {
        User seller = member("pe-seller", 0L);
        User buyer = member("pe-buyer", 300_000L);
        Product product = onSaleProduct(seller, 300_000L);
        flushAndClear();

        Long txId = paidTransaction(buyer, product);

        assertThat(userRepository.findById(buyer.getId()).orElseThrow().getBalanceKrw()).isZero();
        assertThat(transactionRepository.findById(txId).orElseThrow().getStatus()).isEqualTo(TransactionStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.IN_TRADE);
    }

    @Test
    @DisplayName("한 사람이 두 상품을 연달아 사면 거래 2건 · 잔액은 두 가격만큼 준다")
    void 두_상품을_연달아_산다() {
        User seller = member("pe-seller", 0L);
        User buyer = member("pe-buyer", 1_000_000L);
        Product first = onSaleProduct(seller, 300_000L);
        Product second = onSaleProduct(seller, 200_000L);
        flushAndClear();

        Long firstTx = paidTransaction(buyer, first);
        Long secondTx = paidTransaction(buyer, second);

        assertThat(firstTx).isNotEqualTo(secondTx);
        assertThat(userRepository.findById(buyer.getId()).orElseThrow().getBalanceKrw()).isEqualTo(500_000L);
        assertThat(productRepository.findById(first.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.IN_TRADE);
        assertThat(productRepository.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.IN_TRADE);
    }
}
