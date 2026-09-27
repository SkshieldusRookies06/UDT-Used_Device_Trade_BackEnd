package com.rookies6.udt.acceptance.transaction;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 기능: 송장 입력 API {@code PATCH /api/transactions/{id}/shipping}(SPEC §4.7) — BE-D 0절 ⑥·⑨·⑩·⑪.
 * 지금 red면 대개 PATCH 매핑이 없는 것(POST라서 405 → 현재 Advice가 500으로 감싼다).
 */
@AcceptanceTest
@DisplayName("[BE-D ⑥⑨⑩⑪] 송장 입력 API — PATCH · 판매자만 · 400 fields · 공통 봉투")
class ShippingApiTest extends AcceptanceSupport {

    private User seller;
    private User buyer;
    private Long txId;

    @BeforeEach
    void setUp() {
        seller = member("sa-seller", 0L);
        buyer = member("sa-buyer", 1_000_000L);
        txId = paidTransaction(buyer, onSaleProduct(seller, 300_000L));
    }

    private org.springframework.test.web.servlet.ResultActions ship(User as, String body) throws Exception {
        return mvc.perform(patch("/api/transactions/{id}/shipping", txId)
                .with(authentication(memberAuth(as)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("⑨⑩⑪ 판매자가 PATCH로 송장 입력 → 200 · data.status SHIPPING · courier·trackingNo 반영")
    void 판매자가_PATCH로_송장을_입력한다() throws Exception {
        ship(seller, "{\"courier\":\"CJ대한통운\",\"trackingNo\":\"123456789012\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SHIPPING")))
                .andExpect(jsonPath("$.data.courier", is("CJ대한통운")))
                .andExpect(jsonPath("$.data.trackingNo", is("123456789012")));
    }

    @Test
    @DisplayName("⑥⑨ trackingNo 'abc' → 400 VALIDATION_ERROR + fields[].name = trackingNo")
    void 잘못된_송장번호는_400_fields() throws Exception {
        ship(seller, "{\"courier\":\"CJ대한통운\",\"trackingNo\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[*].name", hasItem("trackingNo")));
    }

    @Test
    @DisplayName("⑨⑪ 구매자가 송장을 입력하려 하면 403 TRANSACTION_FORBIDDEN — 판매자 판정은 로그인 사용자로")
    void 구매자는_송장을_입력할_수_없다() throws Exception {
        ship(buyer, "{\"courier\":\"CJ대한통운\",\"trackingNo\":\"123456789012\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TRANSACTION_FORBIDDEN")));
    }
}
