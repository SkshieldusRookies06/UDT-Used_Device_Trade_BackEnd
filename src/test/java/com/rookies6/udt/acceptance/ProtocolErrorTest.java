package com.rookies6.udt.acceptance;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 계약 밖 요청(405·415)도 공통 봉투로 돌아와야 한다. 500으로 감싸이면 통합에서 원인이 가려진다 —
 * 실제로 9/28 BE-D의 POST/PATCH 불일치가 500으로 보여 진단이 늦어졌다.
 */
@AcceptanceTest
@DisplayName("[BE-A] 계약 밖 요청 — 405·415는 500이 아니라 제 상태 코드로 나온다")
class ProtocolErrorTest extends AcceptanceSupport {

    @Test
    @DisplayName("PATCH 전용 엔드포인트에 POST → 405 METHOD_NOT_ALLOWED (봉투 유지)")
    void 잘못된_메서드는_405() throws Exception {
        User buyer = member("pe-buyer", 1_000_000L);
        Long txId = paidTransaction(buyer, onSaleProduct(member("pe-seller", 0L), 100_000L));

        mvc.perform(post("/api/transactions/{id}/confirm", txId)
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("METHOD_NOT_ALLOWED")))
                .andExpect(jsonPath("$.statusCode", is(405)))
                .andExpect(jsonPath("$", hasKey("fields")))
                .andExpect(jsonPath("$.fields", nullValue()));
    }

    @Test
    @DisplayName("multipart 엔드포인트에 JSON → 415 UNSUPPORTED_MEDIA_TYPE (봉투 유지)")
    void 잘못된_타입은_415() throws Exception {
        User seller = member("pe-json", 0L);

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"json으로 보냈다\"}")
                        .with(authentication(memberAuth(seller))))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_MEDIA_TYPE")))
                .andExpect(jsonPath("$", hasKey("fields")))          // 봉투 규약: fields 키는 항상 있다
                .andExpect(jsonPath("$.fields", nullValue()));
    }
}
