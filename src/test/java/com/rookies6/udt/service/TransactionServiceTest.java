package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.TransactionResponse;
import com.rookies6.udt.entity.*;
import com.rookies6.udt.repository.ProductRepository;
import com.rookies6.udt.repository.TransactionRepository;
import com.rookies6.udt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User seller;
    private User buyer;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        seller = User.builder()
                .email("seller@test.com").password("pw").nickname("seller")
                .role(Role.MEMBER).balanceKrw(0L)
                .build();
        setId(seller, 1L);

        buyer = User.builder()
                .email("buyer@test.com").password("pw").nickname("buyer")
                .role(Role.MEMBER).balanceKrw(100_000L)
                .build();
        setId(buyer, 2L);

        product = Product.builder()
                .seller(seller).category(null)
                .title("맥북").description("설명")
                .priceKrw(50_000L).conditionGrade(null)
                .build(); // 기본 status = INSPECTING
        setId(product, 10L);
        setStatus(product, ProductStatus.ON_SALE);
    }

    // ── BR-03 purchase ─────────────────────────────────────

    @Test
    @DisplayName("판매중이 아니면 구매할 수 없다")
    void 판매중이_아니면_구매할_수_없다() {
        setStatus(product, ProductStatus.IN_TRADE);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> transactionService.purchase(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_ON_SALE);
    }

    @Test
    @DisplayName("본인 상품은 살 수 없다")
    void 본인_상품은_살_수_없다() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> transactionService.purchase(1L, 10L)) // sellerId == buyerId
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SELF_PURCHASE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("잔액이 부족하면 구매가 실패하고 잔액이 그대로다")
    void 잔액이_부족하면_구매가_실패하고_잔액이_그대로다() {
        buyer.withdraw(60_000L); // 잔액 40,000원 (가격 50,000원보다 부족)
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> transactionService.purchase(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);

        assertThat(buyer.getBalanceKrw()).isEqualTo(40_000L); // 안 줄어듦
    }

    @Test
    @DisplayName("구매하면 구매자 잔액만 줄고 판매자 잔액은 그대로다")
    void 구매하면_구매자_잔액만_줄고_판매자_잔액은_그대로다() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(productRepository.transition(10L, ProductStatus.ON_SALE, ProductStatus.IN_TRADE))
                .thenReturn(1); // 영향받은 행 1개 = 성공
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.purchase(2L, 10L);

        assertThat(buyer.getBalanceKrw()).isEqualTo(50_000L);  // 100,000 - 50,000
        assertThat(seller.getBalanceKrw()).isEqualTo(0L);       // 그대로 — 에스크로 핵심
        assertThat(response.status()).isEqualTo(TransactionStatus.PAID.name());
        assertThat(response.buyerId()).isEqualTo("2");
        assertThat(response.sellerId()).isEqualTo("1");
    }

    @Test
    @DisplayName("같은 상품을 두 번 사면 두 번째가 실패한다")
    void 같은_상품을_두_번_사면_두_번째가_실패한다() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));
        // 첫 번째 호출은 성공(1), 두 번째는 이미 IN_TRADE라 영향받은 행 0
        when(productRepository.transition(10L, ProductStatus.ON_SALE, ProductStatus.IN_TRADE))
                .thenReturn(1, 0);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.purchase(2L, 10L); // 첫 번째 성공

        assertThatThrownBy(() -> transactionService.purchase(2L, 10L)) // 두 번째
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_ON_SALE);
    }

    // ── BR-05 confirm ──────────────────────────────────────

    @Test
    @DisplayName("구매확정하면 판매자 잔액이 늘고 상품이 SOLD가 된다")
    void 구매확정하면_판매자_잔액이_늘고_상품이_SOLD가_된다() throws Exception {
        Transaction tx = Transaction.builder()
                .product(product).buyer(buyer).amountKrw(50_000L)
                .build();
        setId(tx, 100L);
        tx.registerShipping("CJ대한통운", "123456"); // PAID → SHIPPING
        setStatus(product, ProductStatus.IN_TRADE);

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(tx));

        TransactionResponse response = transactionService.confirm(2L, 100L);

        assertThat(seller.getBalanceKrw()).isEqualTo(50_000L);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThat(response.status()).isEqualTo(TransactionStatus.CONFIRMED.name());
    }

    @Test
    @DisplayName("남의 거래를 확정할 수 없다")
    void 남의_거래를_확정할_수_없다() throws Exception {
        Transaction tx = Transaction.builder()
                .product(product).buyer(buyer).amountKrw(50_000L)
                .build();
        setId(tx, 100L);
        tx.registerShipping("CJ대한통운", "123456");

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.confirm(999L, 100L)) // 구매자 아닌 id
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRANSACTION_FORBIDDEN);
    }

    @Test
    @DisplayName("PAID 상태에서 바로 확정할 수 없다")
    void PAID_상태에서_바로_확정할_수_없다() throws Exception {
        Transaction tx = Transaction.builder()
                .product(product).buyer(buyer).amountKrw(50_000L)
                .build(); // status = PAID (송장 등록 전)
        setId(tx, 100L);

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.confirm(2L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TRANSACTION_STATUS);
    }

    // ── 헬퍼: protected 생성자라 setter 없는 id/status를 리플렉션으로 세팅 ──

    private void setId(Object entity, Long id) throws Exception {
        Field field = findField(entity.getClass(), "id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    private void setStatus(Product product, ProductStatus status) {
        try {
            Field field = findField(Product.class, "status");
            field.setAccessible(true);
            field.set(product, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        return clazz.getDeclaredField(name);
    }
}