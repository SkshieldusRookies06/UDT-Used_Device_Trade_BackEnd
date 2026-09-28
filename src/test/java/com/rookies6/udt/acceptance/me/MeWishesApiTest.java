package com.rookies6.udt.acceptance.me;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.entity.Wish;
import com.rookies6.udt.repository.WishRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 내 찜 목록 {@code GET /api/me/wishes}(T-021 · SPEC §4.9) — BE-A 0절 ③.
 * 상품 목록과 같은 ProductSummary 원소를 쓴다(새 DTO를 만들지 않는다).
 */
@AcceptanceTest
@DisplayName("[BE-A ③] 내 찜 목록 — 남의 찜은 보이지 않는다")
class MeWishesApiTest extends AcceptanceSupport {

    @Autowired private WishRepository wishRepository;

    @Test
    @DisplayName("③ 시드 buyer1 → 2건")
    void 시드_구매자는_2건() throws Exception {
        mvc.perform(get("/api/me/wishes").with(authentication(memberAuth(seedUser("buyer1@udt.test")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.page.totalElements", is(2)));
    }

    @Test
    @DisplayName("③ 내 찜 1건만 — 남이 찜한 상품은 빠진다 · 원소는 ProductSummary")
    void 내_찜만_보인다() throws Exception {
        User seller = member("mw-seller", 0L);
        User me = member("mw-me", 0L);
        User other = member("mw-other", 0L);
        Product mine = onSaleProduct(seller, 150_000L);
        Product theirs = onSaleProduct(seller, 250_000L);
        wishRepository.saveAndFlush(Wish.builder().user(me).product(mine).build());
        wishRepository.saveAndFlush(Wish.builder().user(other).product(theirs).build());
        flushAndClear();

        mvc.perform(get("/api/me/wishes").with(authentication(memberAuth(me))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(String.valueOf(mine.getId()))))
                .andExpect(jsonPath("$.data.content[0].priceKrw", is(150000)));
    }

    @Test
    @DisplayName("③ 비로그인 → 401 AUTHENTICATION_REQUIRED")
    void 비로그인은_401() throws Exception {
        mvc.perform(get("/api/me/wishes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }
}
