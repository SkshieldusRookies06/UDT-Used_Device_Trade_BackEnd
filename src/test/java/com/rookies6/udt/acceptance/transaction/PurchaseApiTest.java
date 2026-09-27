package com.rookies6.udt.acceptance.transaction;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 구매 API {@code POST /api/products/{id}/purchase}(SPEC §4.5) — BE-D 0절 ⑩(공통 봉투)·⑪(CurrentUser).
 */
@AcceptanceTest
@DisplayName("[BE-D ⑩⑪] 구매 API — 201 · 공통 봉투 · 구매자는 로그인 사용자")
class PurchaseApiTest extends AcceptanceSupport {

    private User seller;
    private User buyer;
    private User stranger;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = member("pa-seller", 0L);
        buyer = member("pa-buyer", 1_000_000L);
        stranger = member("pa-stranger", 1_000_000L);
        product = onSaleProduct(seller, 300_000L);
        flushAndClear();
    }

    @Test
    @DisplayName("⑩⑪ 로그인 사용자가 구매 → 201 · {success:true, data:{status:PAID, buyerId}, timestamp}")
    void 구매하면_201과_공통_봉투() throws Exception {
        mvc.perform(post("/api/products/{id}/purchase", product.getId()).with(authentication(memberAuth(buyer))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PAID")))
                .andExpect(jsonPath("$.data.buyerId", is(String.valueOf(buyer.getId()))))
                .andExpect(jsonPath("$.data.sellerId", is(String.valueOf(seller.getId()))))
                .andExpect(jsonPath("$", hasKey("timestamp")));
    }

    @Test
    @DisplayName("⑪ buyerId 파라미터로 남을 구매자로 만들 수 없다 — 파라미터는 무시되고 로그인 사용자가 구매자 (§8 B6)")
    void 구매자는_파라미터가_아니라_로그인에서_온다() throws Exception {
        mvc.perform(post("/api/products/{id}/purchase", product.getId())
                        .with(authentication(memberAuth(buyer)))
                        .param("buyerId", String.valueOf(stranger.getId()))) // 위조 시도
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.buyerId", is(String.valueOf(buyer.getId()))));
    }

    @Test
    @DisplayName("(지킴) 로그인 없이 구매 → 401 AUTHENTICATION_REQUIRED")
    void 로그인_없이_구매하면_401() throws Exception {
        mvc.perform(post("/api/products/{id}/purchase", product.getId()).param("buyerId", String.valueOf(buyer.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }
}
