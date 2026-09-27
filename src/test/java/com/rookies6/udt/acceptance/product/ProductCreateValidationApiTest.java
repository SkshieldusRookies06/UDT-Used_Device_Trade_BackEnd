package com.rookies6.udt.acceptance.product;

import static org.hamcrest.Matchers.hasItem;
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
import org.springframework.test.web.servlet.ResultActions;

/**
 * 기능: 상품 등록 입력값이 틀리면 HTTP로 400 + fields[](SPEC §0 · §4.4) — BE-C 0절 ①.
 * 검증은 컨트롤러 본문보다 먼저 돌기 때문에 ⑤(CurrentUser)와 무관하게 판정된다.
 */
@AcceptanceTest
@DisplayName("[BE-C ①] 상품 등록 API 입력값 — 500이 아니라 400 VALIDATION_ERROR + fields")
class ProductCreateValidationApiTest extends AcceptanceSupport {

    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        seller = member("cv-seller", 0L);
        category = category();
        flushAndClear();
    }

    private ResultActions create(String productJson) throws Exception {
        return mvc.perform(multipart("/api/products")
                .file(new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE,
                        productJson.getBytes(StandardCharsets.UTF_8)))
                .with(authentication(memberAuth(seller))));
    }

    @Test
    @DisplayName("① conditionGrade 'X' → 400 VALIDATION_ERROR + fields[conditionGrade] (지금은 500)")
    void 잘못된_등급은_400() throws Exception {
        create("{\"title\":\"등록 테스트\",\"description\":\"설명입니다\",\"priceKrw\":100000,"
                + "\"conditionGrade\":\"X\",\"categoryId\":\"" + category.getId() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[*].name", hasItem("conditionGrade")));
    }

    @Test
    @DisplayName("① priceKrw 누락 → 400 + fields[priceKrw] (지금은 500)")
    void 가격_누락은_400() throws Exception {
        create("{\"title\":\"등록 테스트\",\"description\":\"설명입니다\","
                + "\"conditionGrade\":\"A\",\"categoryId\":\"" + category.getId() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[*].name", hasItem("priceKrw")));
    }
}
