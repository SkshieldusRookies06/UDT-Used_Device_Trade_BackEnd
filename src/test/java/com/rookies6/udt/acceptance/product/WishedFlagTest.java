package com.rookies6.udt.acceptance.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.entity.Wish;
import com.rookies6.udt.repository.WishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 찜과 상세의 wished(SPEC §4.3 · T-008 "로그인 사용자 기준") — BE-C 0절 ④(서비스)·⑤(상세 컨트롤러)·⑥(WishController).
 * 상세 쪽 테스트는 찜을 저장소로 직접 만들어 ⑥과 분리했다.
 */
@AcceptanceTest
@DisplayName("[BE-C ④⑤⑥] 찜 — 찜하기·해제는 로그인 사용자 · 상세 wished는 보는 사람 기준")
class WishedFlagTest extends AcceptanceSupport {

    @Autowired private WishRepository wishRepository;

    private User seller;
    private User wisher;
    private User other;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = member("wf-seller", 0L);
        wisher = member("wf-wisher", 0L);
        other = member("wf-other", 0L);
        product = onSaleProduct(seller, 50_000L);
        flushAndClear();
    }

    private void wishedBy(User user) {
        wishRepository.saveAndFlush(Wish.builder()
                .user(userRepository.findById(user.getId()).orElseThrow())
                .product(productRepository.findById(product.getId()).orElseThrow()).build());
        flushAndClear();
    }

    @Test
    @DisplayName("⑥ 로그인 사용자가 찜하기 → 201 · wished true · 찜 행의 주인은 로그인 사용자")
    void 찜하기는_로그인_사용자로() throws Exception {
        mvc.perform(post("/api/products/{id}/wishes", product.getId()).with(authentication(memberAuth(wisher))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.wished", is(true)))
                .andExpect(jsonPath("$.data.wishCount", is(1)));

        assertThat(wishRepository.existsByUserIdAndProductId(wisher.getId(), product.getId())).isTrue();
    }

    @Test
    @DisplayName("⑥ 로그인 사용자가 찜 해제 → 200 · wished false")
    void 찜_해제도_로그인_사용자로() throws Exception {
        wishedBy(wisher);

        mvc.perform(delete("/api/products/{id}/wishes", product.getId()).with(authentication(memberAuth(wisher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wished", is(false)))
                .andExpect(jsonPath("$.data.wishCount", is(0)));
    }

    @Test
    @DisplayName("④⑤ 찜한 사람이 상세를 보면 wished true")
    void 찜한_사람에게는_true() throws Exception {
        wishedBy(wisher);

        mvc.perform(get("/api/products/{id}", product.getId()).with(authentication(memberAuth(wisher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wished", is(true)))
                .andExpect(jsonPath("$.data.wishCount", is(1)));
    }

    @Test
    @DisplayName("(지킴) 찜 안 한 다른 회원 → wished false")
    void 다른_회원에게는_false() throws Exception {
        wishedBy(wisher);

        mvc.perform(get("/api/products/{id}", product.getId()).with(authentication(memberAuth(other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wished", is(false)));
    }

    @Test
    @DisplayName("(지킴) 비로그인 상세 → 200 · wished false (401이 아니다)")
    void 비로그인은_false() throws Exception {
        wishedBy(wisher);

        mvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wished", is(false)));
    }
}
