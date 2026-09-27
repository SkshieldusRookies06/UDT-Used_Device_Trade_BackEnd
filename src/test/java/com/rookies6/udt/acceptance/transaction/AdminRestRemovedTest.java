package com.rookies6.udt.acceptance.transaction;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 관리자 강제 처리는 REST로 열지 않는다(SPEC §2.5 · §3.2 · §8 B5) — BE-D 0절 ⑫.
 * /api/** 체인은 역할을 보지 않으므로, 열려 있으면 일반 회원이 강제 환불을 할 수 있다.
 * 관리자 처리는 T-018(BE-B) Thymeleaf 컨트롤러가 TransactionService를 직접 호출한다.
 */
@AcceptanceTest
@DisplayName("[BE-D ⑫] 관리자 REST 제거 — /api/admin/** 강제 환불·확정은 404")
class AdminRestRemovedTest extends AcceptanceSupport {

    private User buyer;
    private Long txId;

    @BeforeEach
    void setUp() {
        buyer = member("ar-buyer", 1_000_000L);
        txId = paidTransaction(buyer, onSaleProduct(member("ar-seller", 0L), 300_000L));
    }

    @Test
    @DisplayName("⑫ 일반 회원이 POST /api/admin/transactions/{id}/force-refund → 404")
    void 강제_환불_REST는_없다() throws Exception {
        mvc.perform(post("/api/admin/transactions/{id}/force-refund", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("⑫ 일반 회원이 POST /api/admin/transactions/{id}/force-confirm → 404")
    void 강제_확정_REST는_없다() throws Exception {
        mvc.perform(post("/api/admin/transactions/{id}/force-confirm", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isNotFound());
    }
}
