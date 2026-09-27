package com.rookies6.udt.acceptance.transaction;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Dispute;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.DisputeRepository;
import com.rookies6.udt.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 거래 상세 API {@code GET /api/transactions/{id}}(SPEC §4.9) — BE-D 0절 ④(서비스)·⑬(컨트롤러).
 * FE-C 거래 상세 화면의 첫 호출. 당사자만 · dispute 키는 항상 있고 없으면 null.
 */
@AcceptanceTest
@DisplayName("[BE-D ④⑬] 거래 상세 API — 당사자만 · dispute 키(없으면 null)")
class TransactionDetailApiTest extends AcceptanceSupport {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private DisputeRepository disputeRepository;

    private User seller;
    private User buyer;
    private User stranger;
    private Long txId;

    @BeforeEach
    void setUp() {
        seller = member("td-seller", 0L);
        buyer = member("td-buyer", 1_000_000L);
        stranger = member("td-stranger", 0L);
        txId = paidTransaction(buyer, onSaleProduct(seller, 300_000L));
    }

    @Test
    @DisplayName("④⑬ 구매자 → 200 · data.id · productTitle · dispute 키가 있고 null")
    void 구매자는_상세를_본다() throws Exception {
        mvc.perform(get("/api/transactions/{id}", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(String.valueOf(txId))))
                .andExpect(jsonPath("$.data.productTitle", is(PRODUCT_TITLE)))
                .andExpect(jsonPath("$.data", hasKey("dispute")))
                .andExpect(jsonPath("$.data.dispute", nullValue()));
    }

    @Test
    @DisplayName("④⑬ 판매자 → 200")
    void 판매자도_상세를_본다() throws Exception {
        mvc.perform(get("/api/transactions/{id}", txId).with(authentication(memberAuth(seller))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sellerId", is(String.valueOf(seller.getId()))));
    }

    @Test
    @DisplayName("④ 제3자 → 403 TRANSACTION_FORBIDDEN")
    void 제3자는_403() throws Exception {
        mvc.perform(get("/api/transactions/{id}", txId).with(authentication(memberAuth(stranger))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TRANSACTION_FORBIDDEN")));
    }

    @Test
    @DisplayName("④ 없는 거래 → 404 TRANSACTION_NOT_FOUND (RESOURCE_NOT_FOUND면 GET 매핑이 없는 것)")
    void 없는_거래는_404() throws Exception {
        mvc.perform(get("/api/transactions/{id}", 999_999_999L).with(authentication(memberAuth(buyer))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TRANSACTION_NOT_FOUND")));
    }

    @Test
    @DisplayName("④⑦ 분쟁 중인 거래 → data.dispute가 객체(status OPEN · reason)")
    void 분쟁_중이면_dispute_객체가_붙는다() throws Exception {
        transactionService.markDisputed(buyer.getId(), txId);
        disputeRepository.saveAndFlush(Dispute.builder()
                .transaction(transactionRepository.findById(txId).orElseThrow())
                .reporter(userRepository.findById(buyer.getId()).orElseThrow())
                .reason("박스만 왔습니다").build());
        flushAndClear();

        mvc.perform(get("/api/transactions/{id}", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DISPUTED")))
                .andExpect(jsonPath("$.data.dispute.status", is("OPEN")))
                .andExpect(jsonPath("$.data.dispute.reason", is("박스만 왔습니다")))
                .andExpect(jsonPath("$.data.dispute.transactionId", is(String.valueOf(txId))));
    }
}
