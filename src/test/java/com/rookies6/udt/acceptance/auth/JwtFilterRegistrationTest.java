package com.rookies6.udt.acceptance.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: JWT 필터 배선(SPEC §7) — BE-B 0절 ①.
 * T-004는 필터를 만들기만 했고 SecurityConfig.apiFilterChain에 등록하지 않았다 → 토큰을 보내도 항상 401.
 * 로그인 API가 아직 없어도 되게 토큰은 JwtTokenProvider로 직접 만든다.
 * 보호 경로로는 "없는 거래 조회"를 쓴다 — 인증을 통과하면 404, 못 하면 401.
 */
@AcceptanceTest
@DisplayName("[BE-B ①] JWT 필터 등록 — Bearer 토큰이 /api/** 에서 인증으로 바뀐다")
class JwtFilterRegistrationTest extends AcceptanceSupport {

    private static final String PROTECTED = "/api/transactions/999999999";

    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("① 유효한 Bearer 토큰 → 401이 아니다 (인증 통과 뒤 없는 거래라 404 · 401이면 필터 미등록)")
    void 토큰이_있으면_인증된다() throws Exception {
        User user = member("jf", 0L);
        String token = jwtTokenProvider.createToken(user.getId(), "MEMBER"); // 2-인자 — ②에서도 지우지 않는다

        mvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token))
                .andExpect(status().is(not(401)));
    }

    @Test
    @DisplayName("(지킴) 토큰 없음 → 401 AUTHENTICATION_REQUIRED JSON 봉투 (HTML 리다이렉트가 아니다)")
    void 토큰이_없으면_401() throws Exception {
        mvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }

    @Test
    @DisplayName("(지킴) 깨진 토큰 → 예외 없이 무인증 처리 → 401 (500이 아니다)")
    void 깨진_토큰은_401() throws Exception {
        mvc.perform(get(PROTECTED).header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_REQUIRED")));
    }
}
