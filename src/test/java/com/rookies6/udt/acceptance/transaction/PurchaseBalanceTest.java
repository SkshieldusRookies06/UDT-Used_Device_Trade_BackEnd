package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import com.rookies6.udt.entity.TransactionStatus;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 구매(BR-03)의 돈과 상태 — BE-D 0절 ①·②.
 * Mockito 테스트는 JPA를 안 거쳐서 못 잡는 것을 실제 DB로 본다.
 *
 * <ul>
 *   <li>① {@code transition()}의 {@code clearAutomatically = true} → 벌크 UPDATE 뒤 컨텍스트가 비워져
 *       {@code buyer.withdraw()}가 DB에 안 들어간다. 지우면 {@code 잔액이_DB에서_줄어든다} green.</li>
 *   <li>② ①만 하면 {@code 같은_요청_안에서_상품은_IN_TRADE다}가 red로 바뀐다 — 벌크 UPDATE는 메모리 객체를 안 바꾸므로
 *       {@code product.changeStatus(IN_TRADE)}를 한 줄 더 넣어야 green.</li>
 * </ul>
 */
@AcceptanceTest
@DisplayName("[BE-D ①②] 구매 — 잔액 차감·상품 상태가 DB와 메모리에 맞게 남는다")
class PurchaseBalanceTest extends AcceptanceSupport {

    @Autowired private TransactionRepository transactionRepository;

    private User seller;
    private User buyer;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = member("pb-seller", 0L);
        buyer = member("pb-buyer", 1_000_000L);
        product = onSaleProduct(seller, 300_000L);
        flushAndClear(); // 서비스가 DB에서 새로 읽게 한다 — 실제 요청과 같은 조건
    }

    @Test
    @DisplayName("① 구매하면 구매자 잔액이 DB에서 실제로 줄어든다 (1,000,000 → 700,000) · 거래 PAID · 상품 IN_TRADE")
    void 잔액이_DB에서_줄어든다() {
        Long txId = paidTransaction(buyer, product);

        assertThat(userRepository.findById(buyer.getId()).orElseThrow().getBalanceKrw())
                .as("구매자 잔액 — 700,000이 아니라 1,000,000이면 clearAutomatically 때문에 withdraw()가 유실된 것")
                .isEqualTo(700_000L);
        assertThat(transactionRepository.findById(txId).orElseThrow().getStatus()).isEqualTo(TransactionStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.IN_TRADE);
    }

    @Test
    @DisplayName("(지킴·중간) ② 같은 요청 안에서 다시 읽어도 상품은 IN_TRADE다 — 지금은 초록 · ①만 하면 빨강 · ②까지 하면 다시 초록")
    void 같은_요청_안에서_상품은_IN_TRADE다() {
        transactionService.purchase(buyer.getId(), product.getId());
        // flush·clear 하지 않는다 — 서비스가 쓰던 영속 객체를 그대로 본다

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus())
                .as("ON_SALE이면 purchase()에서 transition() 뒤 product.changeStatus(IN_TRADE)가 빠진 것")
                .isEqualTo(ProductStatus.IN_TRADE);
    }

    @Test
    @DisplayName("(지킴) 잔액이 모자라면 INSUFFICIENT_BALANCE이고 상품·잔액·거래가 그대로다")
    void 잔액_부족이면_아무것도_안_바뀐다() {
        User poor = member("pb-poor", 1_000L);
        flushAndClear();

        assertThatThrownBy(() -> transactionService.purchase(poor.getId(), product.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        flushAndClear();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(userRepository.findById(poor.getId()).orElseThrow().getBalanceKrw()).isEqualTo(1_000L);
        assertThat(transactionRepository.findByProductId(product.getId())).isEmpty(); // ⑤ 전(Optional)·후(List) 둘 다 컴파일된다
    }

    @Test
    @DisplayName("(지킴) 에스크로 — 구매 → 송장 → 확정 동안 판매자 잔액은 확정 때 처음으로 는다")
    void 판매자_돈은_확정_때만_움직인다() {
        Long txId = paidTransaction(buyer, product);
        assertThat(userRepository.findById(seller.getId()).orElseThrow().getBalanceKrw()).isZero();

        transactionService.registerShipping(seller.getId(), txId,
                new com.rookies6.udt.dto.ShippingRequest("CJ대한통운", "123456789012"));
        flushAndClear();
        assertThat(userRepository.findById(seller.getId()).orElseThrow().getBalanceKrw()).isZero();

        transactionService.confirm(buyer.getId(), txId);
        flushAndClear();
        assertThat(userRepository.findById(seller.getId()).orElseThrow().getBalanceKrw()).isEqualTo(300_000L);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.SOLD);
    }
}
