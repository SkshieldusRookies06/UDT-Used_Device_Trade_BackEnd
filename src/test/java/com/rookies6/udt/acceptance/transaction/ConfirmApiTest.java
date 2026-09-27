package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.dto.ShippingRequest;
import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 구매 확정 API {@code PATCH /api/transactions/{id}/confirm}(SPEC §4.6) — BE-D 0절 ⑨·⑩·⑪.
 */
@AcceptanceTest
@DisplayName("[BE-D ⑨⑩⑪] 구매 확정 API — PATCH · 구매자만 · SHIPPING에서만 · 공통 봉투")
class ConfirmApiTest extends AcceptanceSupport {

    private User seller;
    private User buyer;
    private Long txId;

    @BeforeEach
    void setUp() {
        seller = member("ca-seller", 0L);
        buyer = member("ca-buyer", 1_000_000L);
        txId = paidTransaction(buyer, onSaleProduct(seller, 300_000L));
    }

    private void shipped() {
        transactionService.registerShipping(seller.getId(), txId, new ShippingRequest("CJ대한통운", "123456789012"));
        flushAndClear();
    }

    @Test
    @DisplayName("⑨⑩⑪ 배송 중 거래를 구매자가 PATCH로 확정 → 200 · data.status CONFIRMED · 판매자 잔액 +300,000")
    void 구매자가_PATCH로_확정한다() throws Exception {
        shipped();

        mvc.perform(patch("/api/transactions/{id}/confirm", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));

        flushAndClear();
        assertThat(userRepository.findById(seller.getId()).orElseThrow().getBalanceKrw()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("⑨⑩ PAID(배송 전) 거래를 확정 → 409 INVALID_TRANSACTION_STATUS + 공통 에러 봉투(success·statusCode·code·fields)")
    void 배송_전_확정은_409() throws Exception {
        mvc.perform(patch("/api/transactions/{id}/confirm", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(409)))
                .andExpect(jsonPath("$.code", is("INVALID_TRANSACTION_STATUS")))
                .andExpect(jsonPath("$", hasKey("fields")));
    }

    @Test
    @DisplayName("⑨⑪ 판매자가 확정하려 하면 403 TRANSACTION_FORBIDDEN — 구매자 판정은 로그인 사용자로")
    void 판매자는_확정할_수_없다() throws Exception {
        shipped();

        mvc.perform(patch("/api/transactions/{id}/confirm", txId).with(authentication(memberAuth(seller))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TRANSACTION_FORBIDDEN")));
    }
}
