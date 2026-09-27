package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.TransactionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 환불 후 재구매(SPEC §2.4) — BE-D 0절 ⑤.
 * 한 상품에 거래가 여러 건 생길 수 있으므로 {@code findByProductId}는 Optional이 아니라 List여야 한다.
 * Optional이면 2건째에서 IncorrectResultSizeDataAccessException.
 */
@AcceptanceTest
@DisplayName("[BE-D ⑤] 환불 후 재구매 — 상품당 거래 여러 건을 조회할 수 있다")
class RepurchaseAfterRefundTest extends AcceptanceSupport {

    @Autowired private TransactionRepository transactionRepository;

    @Test
    @DisplayName("⑤ 구매 → 분쟁 → 강제 환불 → 다른 사람이 재구매하면 findByProductId가 2건을 돌려준다")
    void 재구매하면_거래가_두_건이다() {
        User seller = member("rp-seller", 0L);
        User first = member("rp-first", 1_000_000L);
        User second = member("rp-second", 1_000_000L);
        Product product = onSaleProduct(seller, 100_000L);
        flushAndClear();

        Long firstTx = paidTransaction(first, product);
        transactionService.markDisputed(first.getId(), firstTx);
        flushAndClear();
        transactionService.forceRefund(firstTx); // 상품이 다시 ON_SALE
        flushAndClear();
        paidTransaction(second, product);

        Object found = transactionRepository.findByProductId(product.getId()); // 반환형이 바뀌어도 컴파일되게 Object로 받는다
        assertThat(found).as("TransactionRepository.findByProductId 반환형").isInstanceOf(List.class);
        assertThat((List<?>) found).hasSize(2);
    }
}
