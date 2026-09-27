package com.rookies6.udt.acceptance.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.security.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 기능: 회원가입·로그인·내 정보(SPEC §4.9 · T-005 수용 기준) — BE-B 0절 ③.
 * 개정이 아니라 T-005 본 작업이라 지금은 전부 red(엔드포인트 없음 → 404)가 정상이다.
 * 비밀번호 해시는 BE-B 몫이므로 사용자는 저장소가 아니라 가입 API로 만든다.
 */
@AcceptanceTest
@DisplayName("[BE-B ③] 인증 API — signup 201 · login 200/401 · /api/me")
class AuthApiTest extends AcceptanceSupport {

    private static final String PASSWORD = "Test1234!";

    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String email;

    @BeforeEach
    void setUp() {
        email = "auth-" + UUID.randomUUID().toString().substring(0, 8) + "@udt.test";
    }

    private ResultActions signup(String mail, String nickname) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + mail + "\",\"password\":\"" + PASSWORD + "\",\"nickname\":\"" + nickname + "\"}"));
    }

    private ResultActions login(String mail, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + mail + "\",\"password\":\"" + password + "\"}"));
    }

    private String tokenOf(String mail) throws Exception {
        String body = login(mail, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.accessToken");
    }

    @Test
    @DisplayName("③ 가입 → 201 · data {id, email, nickname} · 비밀번호는 응답에 없다")
    void 가입하면_201() throws Exception {
        signup(email, "가입닉")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.nickname", is("가입닉")))
                .andExpect(jsonPath("$.data", hasKey("id")))
                .andExpect(jsonPath("$.data", not(hasKey("password"))));
    }

    @Test
    @DisplayName("③ 같은 이메일로 다시 가입 → 409 EMAIL_ALREADY_EXISTS")
    void 중복_이메일은_409() throws Exception {
        signup(email, "첫째").andExpect(status().isCreated());

        signup(email, "둘째")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("③ 로그인 → 200 · data {accessToken, user:{id, nickname, role, balanceKrw}} · 토큰 claims에 nickname")
    void 로그인하면_토큰과_사용자() throws Exception {
        signup(email, "로그인닉").andExpect(status().isCreated());

        String body = login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.nickname", is("로그인닉")))
                .andExpect(jsonPath("$.data.user.role", is("MEMBER")))
                .andExpect(jsonPath("$.data.user", hasKey("id")))
                .andExpect(jsonPath("$.data.user", hasKey("balanceKrw")))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.data.accessToken");
        assertThat(jwtTokenProvider.parseClaims(token).get("nickname", String.class)).isEqualTo("로그인닉");
    }

    @Test
    @DisplayName("③ 비밀번호가 틀리거나 없는 이메일 → 둘 다 401 INVALID_CREDENTIALS (계정 열거 방지)")
    void 자격이_틀리면_401() throws Exception {
        signup(email, "틀림닉").andExpect(status().isCreated());

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
        login("no-such-" + email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("③ 로그인 토큰으로 GET /api/me → 200 · data {id, email, nickname, role, balanceKrw}")
    void 토큰으로_내_정보를_본다() throws Exception {
        signup(email, "내닉").andExpect(status().isCreated());
        String token = tokenOf(email);

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(email)))
                .andExpect(jsonPath("$.data.nickname", is("내닉")))
                .andExpect(jsonPath("$.data.role", is("MEMBER")))
                .andExpect(jsonPath("$.data", hasKey("balanceKrw")))
                .andExpect(jsonPath("$.data", not(hasKey("password"))));
    }
}
