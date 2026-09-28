package com.rookies6.udt.acceptance.dispute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.User;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 기능: 증빙 다운로드 {@code GET /api/disputes/{id}/files/{fileId}} · {@code /admin/disputes/...}(SPEC §4.1) — BE-A 0절 ⑤.
 * 두 경로가 같은 저장·권한 코드를 쓴다. 발표의 "증빙 파일 권한 검사"가 이 클래스다.
 */
@AcceptanceTest
@DisplayName("[BE-A ⑤] 증빙 다운로드 — 당사자·관리자만 · attachment 헤더 · 한글 파일명")
class DisputeFileDownloadTest extends AcceptanceSupport {

    private static final String KOREAN_NAME = "증빙 사진.png";

    private User seller;
    private User buyer;
    private User stranger;
    private Long disputeId;
    private Long fileId;

    @BeforeEach
    void setUp() throws Exception {
        seller = member("dd-seller", 0L);
        buyer = member("dd-buyer", 1_000_000L);
        stranger = member("dd-stranger", 0L);
        Long txId = paidTransaction(buyer, onSaleProduct(seller, 400_000L));

        String json = "{\"reason\":\"수령한 제품에 하자가 있어 증빙을 첨부합니다\"}";
        String body = mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(new MockMultipartFile("dispute", "", MediaType.APPLICATION_JSON_VALUE,
                                json.getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("files", KOREAN_NAME, "image/png", PNG_HEAD))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        disputeId = Long.valueOf(JsonPath.read(body, "$.data.id"));
        fileId = Long.valueOf(JsonPath.read(body, "$.data.files[0].id"));
        flushAndClear();
    }

    @Test
    @DisplayName("⑤ 구매자 → 200 · Content-Disposition: attachment · 한글 파일명이 UTF-8로 인코딩된다")
    void 구매자는_내려받는다() throws Exception {
        String disposition = mvc.perform(get("/api/disputes/{id}/files/{fileId}", disputeId, fileId)
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);

        assertThat(disposition).startsWith("attachment;");
        assertThat(disposition).contains("filename*=UTF-8''");
        assertThat(disposition).doesNotContain(KOREAN_NAME.substring(0, 2));
    }

    @Test
    @DisplayName("⑤ 판매자(거래 당사자) → 200")
    void 판매자도_내려받는다() throws Exception {
        mvc.perform(get("/api/disputes/{id}/files/{fileId}", disputeId, fileId)
                        .with(authentication(memberAuth(seller))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("⑤ 제3자 → 403 ACCESS_DENIED")
    void 제3자는_403() throws Exception {
        mvc.perform(get("/api/disputes/{id}/files/{fileId}", disputeId, fileId)
                        .with(authentication(memberAuth(stranger))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("⑤ 관리자 화면 경로 /admin/disputes/... → 200 (세션 ADMIN · 같은 저장 코드)")
    void 관리자는_관리자경로로_내려받는다() throws Exception {
        mvc.perform(get("/admin/disputes/{id}/files/{fileId}", disputeId, fileId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                        .startsWith("attachment;"));
    }

    @Test
    @DisplayName("(지킴) 비로그인 → 401 · 없는 파일 → 404 DISPUTE_NOT_FOUND")
    void 비로그인과_없는_파일() throws Exception {
        mvc.perform(get("/api/disputes/{id}/files/{fileId}", disputeId, fileId))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/disputes/{id}/files/{fileId}", disputeId, 999_999_999L)
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DISPUTE_NOT_FOUND")));
    }
}
