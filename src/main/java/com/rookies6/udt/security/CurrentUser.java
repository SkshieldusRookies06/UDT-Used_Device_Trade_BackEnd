package com.rookies6.udt.security;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 로그인 사용자 id를 SecurityContext에서 꺼낸다.
 * JwtAuthenticationFilter가 principal에 userId를 문자열로 넣는다(SPEC §7).
 * 컨트롤러는 이 두 메서드만 쓴다 — 각자 SecurityContextHolder를 직접 만지지 않는다.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** 로그인 필수 엔드포인트용. 인증이 없으면 401 AUTHENTICATION_REQUIRED. */
    public static Long id() {
        return idOrNull()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED));
    }

    /** 공개 엔드포인트에서 "로그인했으면 그 사람 기준"이 필요할 때(예: 상품 상세의 wished). */
    public static Optional<Long> idOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }
        String principal = String.valueOf(auth.getPrincipal());
        if ("anonymousUser".equals(principal)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(principal));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
