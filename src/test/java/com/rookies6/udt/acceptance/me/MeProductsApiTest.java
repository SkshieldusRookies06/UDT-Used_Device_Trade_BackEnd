package com.rookies6.udt.acceptance.me;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.ConditionGrade;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.entity.Wish;
import com.rookies6.udt.repository.WishRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 내 상품 목록 {@code GET /api/me/products}(T-021 · SPEC §4.9) — BE-A 0절 ①.
 * 검수대기·반려까지 전 상태가 보이고, 남의 상품은 보이지 않는다.
 */
@AcceptanceTest
@DisplayName("[BE-A ①] 내 상품 목록 — 전 상태 포함 · 남의 상품 제외")
class MeProductsApiTest extends AcceptanceSupport {

    @Autowired private WishRepository wishRepository;

    @Test
    @DisplayName("① 시드 seller1 → 22건 (ON_SALE 15 · INSPECTING 3 · IN_TRADE 3 · SOLD 1)")
    void 시드_판매자는_22건() throws Exception {
        mvc.perform(get("/api/me/products").with(authentication(memberAuth(seedUser("seller1@udt.test")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.page.totalElements", is(22)));
    }

    @Test
    @DisplayName("① 시드 buyer1 → 0건 (등록한 상품이 없다)")
    void 시드_구매자는_0건() throws Exception {
        mvc.perform(get("/api/me/products").with(authentication(memberAuth(seedUser("buyer1@udt.test")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements", is(0)));
    }

    @Test
    @DisplayName("① 내 상품만 2건 — INSPECTING 포함 · 남의 ON_SALE 제외 · id는 문자열")
    void 내_상품만_보인다() throws Exception {
        User me = member("mp-me", 0L);
        User other = member("mp-other", 0L);
        onSaleProduct(me, 100_000L);
        inspectingProduct(me, 200_000L);
        onSaleProduct(other, 300_000L);
        flushAndClear();

        mvc.perform(get("/api/me/products").with(authentication(memberAuth(me))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content[0].id", instanceOf(String.class)));
    }

    @Test
    @DisplayName("① 비로그인 → 401 AUTHENTICATION_REQUIRED")
    void 비로그인은_401() throws Exception {
        mvc.perform(get("/api/me/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }

    @Test
    @DisplayName("① 찜 수는 상품마다 제 값이 붙는다 — 페이지 단위 집계(N+1 아님)")
    void 찜_수가_상품별로_맞는다() throws Exception {
        User me = member("mp-count-seller", 0L);
        Product popular = onSaleProduct(me, 100_000L);
        inspectingProduct(me, 200_000L);                       // 찜 0건
        wishRepository.saveAndFlush(Wish.builder().user(member("mp-w1", 0L)).product(popular).build());
        wishRepository.saveAndFlush(Wish.builder().user(member("mp-w2", 0L)).product(popular).build());
        flushAndClear();

        mvc.perform(get("/api/me/products").param("size", "50")
                        .with(authentication(memberAuth(me))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content[?(@.id=='" + popular.getId() + "')].wishCount", contains(2)));
    }

    private Product inspectingProduct(User seller, long price) {
        return productRepository.saveAndFlush(Product.builder()
                .seller(seller).category(category()).title(PRODUCT_TITLE).description("검수 대기")
                .priceKrw(price).conditionGrade(ConditionGrade.A).build());
    }
}
