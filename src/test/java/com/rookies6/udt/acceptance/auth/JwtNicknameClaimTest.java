package com.rookies6.udt.acceptance.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 토큰 규격(SPEC §1 확정 사항 — claims role·nickname) — BE-B 0절 ②. 스프링 없이 1초.
 * 아직 없는 3-인자 메서드를 직접 부르면 main이 컴파일되지 않으므로 이름으로(리플렉션) 찾는다.
 */
@DisplayName("[BE-B ②] JWT claims — createToken(userId, role, nickname) · 2-인자는 유지")
class JwtNicknameClaimTest {

    private final JwtTokenProvider provider =
            new JwtTokenProvider("test-secret-key-for-udt-acceptance-tests-32bytes!!", 3600);

    @Test
    @DisplayName("② createToken(Long, String, String)으로 만든 토큰에 sub · role · nickname이 들어 있다")
    void 닉네임_claim이_들어간다() throws Exception {
        Method threeArgs = JwtTokenProvider.class.getMethod("createToken", Long.class, String.class, String.class);

        String token = (String) threeArgs.invoke(provider, 7L, "MEMBER", "테스트닉");
        Claims claims = provider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(claims.get("nickname", String.class)).isEqualTo("테스트닉");
    }

    @Test
    @DisplayName("(지킴) 2-인자 createToken(Long, String)은 그대로 있고 sub · role이 들어 있다")
    void 두_인자도_그대로_된다() {
        String token = provider.createToken(7L, "MEMBER");

        assertThat(provider.getUserId(token)).isEqualTo(7L);
        assertThat(provider.getRole(token)).isEqualTo("MEMBER");
    }
}
