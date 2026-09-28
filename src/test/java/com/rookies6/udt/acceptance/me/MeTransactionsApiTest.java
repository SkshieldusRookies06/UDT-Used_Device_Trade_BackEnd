package com.rookies6.udt.acceptance.me;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 내 거래 목록 {@code GET /api/me/transactions?role=buyer|seller}(T-021 · SPEC §4.9) — BE-A 0절 ②.
 * role은 필수이고 두 값만 허용한다. 목 서버도 같은 400을 돌려준다.
 */
@AcceptanceTest
@DisplayName("[BE-A ②] 내 거래 목록 — role=buyer|seller · role 누락·오류는 400")
class MeTransactionsApiTest extends AcceptanceSupport {

    @Test
    @DisplayName("② 시드 buyer1 role=buyer → 4건 (PAID·SHIPPING·CONFIRMED·DISPUTED)")
    void 시드_구매자는_4건() throws Exception {
        mvc.perform(get("/api/me/transactions").param("role", "buyer")
                        .with(authentication(memberAuth(seedUser("buyer1@udt.test")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.page.totalElements", is(4)));
    }

    @Test
    @DisplayName("② 시드 seller1 role=seller → 4건")
    void 시드_판매자는_4건() throws Exception {
        mvc.perform(get("/api/me/transactions").param("role", "seller")
                        .with(authentication(memberAuth(seedUser("seller1@udt.test")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements", is(4)));
    }

    @Test
    @DisplayName("② role 누락 → 400 VALIDATION_ERROR")
    void role_누락은_400() throws Exception {
        mvc.perform(get("/api/me/transactions").with(authentication(memberAuth(seedUser("buyer1@udt.test")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("② role=x → 400 VALIDATION_ERROR")
    void role_오류는_400() throws Exception {
        mvc.perform(get("/api/me/transactions").param("role", "x")
                        .with(authentication(memberAuth(seedUser("buyer1@udt.test")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("② 내 거래만 보인다 — 구매자 1건 · 판매자 1건 · 제3자 0건 · id는 문자열")
    void 내_거래만_보인다() throws Exception {
        User seller = member("mt-seller", 0L);
        User buyer = member("mt-buyer", 1_000_000L);
        User stranger = member("mt-stranger", 0L);
        paidTransaction(buyer, onSaleProduct(seller, 200_000L));

        mvc.perform(get("/api/me/transactions").param("role", "buyer")
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", instanceOf(String.class)))
                .andExpect(jsonPath("$.data.content[0].buyerId", is(String.valueOf(buyer.getId()))))
                .andExpect(jsonPath("$.data.content[0].sellerId", is(String.valueOf(seller.getId()))));

        mvc.perform(get("/api/me/transactions").param("role", "seller")
                        .with(authentication(memberAuth(seller))))
                .andExpect(jsonPath("$.data.page.totalElements", is(1)));

        mvc.perform(get("/api/me/transactions").param("role", "buyer")
                        .with(authentication(memberAuth(stranger))))
                .andExpect(jsonPath("$.data.page.totalElements", is(0)));
    }

    @Test
    @DisplayName("② 비로그인 → 401 AUTHENTICATION_REQUIRED")
    void 비로그인은_401() throws Exception {
        mvc.perform(get("/api/me/transactions").param("role", "buyer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }
}
