package com.rookies6.udt.acceptance.product;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Category;
import com.rookies6.udt.entity.User;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 기능: 상품 등록 API {@code POST /api/products}(SPEC §4.4) — BE-C 0절 ⑤(판매자 = 로그인 사용자).
 * 지금은 {@code Long sellerId = null}이라 로그인해도 500.
 */
@AcceptanceTest
@DisplayName("[BE-C ⑤] 상품 등록 API — 판매자는 로그인 사용자 · 201 · INSPECTING")
class ProductCreateApiTest extends AcceptanceSupport {

    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = member("pc-seller", 0L);
        category = category();
        flushAndClear();
    }

    private MockMultipartFile productPart() {
        String json = "{\"title\":\"등록 테스트\",\"description\":\"설명입니다\",\"priceKrw\":100000,"
                + "\"conditionGrade\":\"A\",\"categoryId\":\"" + category.getId() + "\"}";
        return new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("⑤ 로그인 사용자가 사진 없이 등록 → 201 · data.status INSPECTING · sellerId = 로그인 사용자")
    void 등록하면_판매자는_로그인_사용자() throws Exception {
        mvc.perform(multipart("/api/products").file(productPart()).with(authentication(memberAuth(seller))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INSPECTING")))
                .andExpect(jsonPath("$.data.sellerId", is(String.valueOf(seller.getId()))))
                .andExpect(jsonPath("$.data.wished", is(false)));
    }

    @Test
    @DisplayName("⑤② 로그인 사용자가 사진 2장과 등록 → 201 · data.images 2개")
    void 사진과_함께_등록한다() throws Exception {
        mvc.perform(multipart("/api/products").file(productPart()).file(png("a.png")).file(png("b.png"))
                        .with(authentication(memberAuth(seller))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.images", hasSize(2)));
    }

    @Test
    @DisplayName("(지킴) 로그인 없이 등록 → 401")
    void 로그인_없이_등록하면_401() throws Exception {
        mvc.perform(multipart("/api/products").file(productPart()))
                .andExpect(status().isUnauthorized());
    }
}
