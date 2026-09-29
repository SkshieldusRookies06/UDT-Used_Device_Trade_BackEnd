package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.entity.ConditionGrade;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.ProductStatus;
import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 관리자 검수 승인·반려(BR-01·02) — T-022 수용 기준.
 * 관리자 화면(T-018 · AdminProductController)이 부르는 경로라 REST 게이트가 보지 않는다.
 */
@AcceptanceTest
@DisplayName("[BE-A · T-022] 검수 승인·반려 — INSPECTING에서만 · 승인은 ON_SALE · 반려는 REJECTED + 사유")
class InspectionTest extends AcceptanceSupport {

    private User seller;

    @BeforeEach
    void setUp() {
        seller = member("in-seller", 0L);
    }

    private Product inspectingProduct() {
        Product p = productRepository.saveAndFlush(Product.builder()
                .seller(seller).category(category()).title(PRODUCT_TITLE).description("설명")
                .priceKrw(300_000L).conditionGrade(ConditionGrade.A).build());
        flushAndClear();
        return p;
    }

    @Test
    @DisplayName("승인하면 INSPECTING → ON_SALE")
    void 승인하면_판매중이_된다() {
        Product product = inspectingProduct();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INSPECTING);

        transactionService.approveInspection(product.getId());
        flushAndClear();

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("반려하면 INSPECTING → REJECTED · 사유가 저장된다")
    void 반려하면_사유와_함께_REJECTED() {
        Product product = inspectingProduct();

        transactionService.rejectInspection(product.getId(), "사진이 실물과 다릅니다");
        flushAndClear();

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ProductStatus.REJECTED);
        assertThat(found.getRejectReason()).isEqualTo("사진이 실물과 다릅니다");
    }

    @Test
    @DisplayName("[BE-D] 판매 중인 상품은 승인·반려할 수 없다 — 409 PRODUCT_NOT_INSPECTING · 상태 그대로")
    void 검수_대기가_아니면_409() {
        Product onSale = onSaleProduct(seller, 300_000L);
        flushAndClear();

        assertThatThrownBy(() -> transactionService.approveInspection(onSale.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .as("INVALID_TRANSACTION_STATUS면 TransactionService의 임시 코드가 남은 것")
                .isEqualTo(ErrorCode.PRODUCT_NOT_INSPECTING);
        assertThatThrownBy(() -> transactionService.rejectInspection(onSale.getId(), "사유"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_INSPECTING);

        flushAndClear();
        assertThat(productRepository.findById(onSale.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("없는 상품이면 PRODUCT_NOT_FOUND")
    void 없는_상품이면_404() {
        assertThatThrownBy(() -> transactionService.approveInspection(999_999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
