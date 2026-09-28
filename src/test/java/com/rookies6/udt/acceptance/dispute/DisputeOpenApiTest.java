package com.rookies6.udt.acceptance.dispute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.service.DisputeService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 기능: 분쟁 접수 {@code POST /api/transactions/{id}/disputes}(SPEC §4.8) — BE-A 0절 ④.
 * 전이는 TransactionService.markDisputed가, Dispute 생성·파일 저장은 DisputeService가 한다(§2.5).
 */
@AcceptanceTest
@DisplayName("[BE-A ④] 분쟁 접수 — 구매자만 · PAID|SHIPPING · 거래당 1건 · 증빙 0~3장")
class DisputeOpenApiTest extends AcceptanceSupport {

    @Autowired private DisputeService disputeService;

    private User seller;
    private User buyer;
    private Long txId;

    @BeforeEach
    void setUp() {
        seller = member("dp-seller", 0L);
        buyer = member("dp-buyer", 1_000_000L);
        Product product = onSaleProduct(seller, 300_000L);
        txId = paidTransaction(buyer, product);
    }

    private MockMultipartFile disputePart(String reason) {
        String json = "{\"reason\":\"" + reason + "\"}";
        return new MockMultipartFile("dispute", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile evidence(String name) {
        return new MockMultipartFile("files", name, "image/png", PNG_HEAD);
    }

    @Test
    @DisplayName("④ 구매자가 증빙 1장과 접수 → 201 · files 1개 · 거래는 DISPUTED가 된다")
    void 구매자는_분쟁을_접수한다() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("배송 온 물건 액정에 멍이 있습니다"))
                        .file(evidence("evidence.png"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.transactionId", is(String.valueOf(txId))))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andExpect(jsonPath("$.data.files[0].originalName", is("evidence.png")));

        flushAndClear();

        mvc.perform(get("/api/transactions/{id}", txId).with(authentication(memberAuth(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DISPUTED")))
                .andExpect(jsonPath("$.data.dispute.status", is("OPEN")));
    }

    @Test
    @DisplayName("④ 증빙 없이 접수 → 201 · files 빈 배열")
    void 증빙_없이도_접수된다() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("물건을 받지 못했습니다 확인 부탁드립니다"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.files", hasSize(0)));
    }

    @Test
    @DisplayName("④ 같은 거래에 재접수 → 409 DISPUTE_ALREADY_EXISTS")
    void 재접수는_409() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("첫 번째 분쟁 신고입니다 확인 바랍니다"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isCreated());
        flushAndClear();

        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("두 번째 분쟁 신고입니다 확인 바랍니다"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DISPUTE_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("④ 판매자가 접수 시도 → 403 TRANSACTION_FORBIDDEN")
    void 판매자는_403() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("판매자가 신고를 시도합니다 막혀야 합니다"))
                        .with(authentication(memberAuth(seller))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TRANSACTION_FORBIDDEN")));
    }

    @Test
    @DisplayName("④ CONFIRMED 거래에 접수 → 409 INVALID_TRANSACTION_STATUS")
    void 확정된_거래는_409() throws Exception {
        transactionService.registerShipping(seller.getId(), txId,
                new com.rookies6.udt.dto.ShippingRequest("CJ대한통운", "123456789012"));
        transactionService.confirm(buyer.getId(), txId);
        flushAndClear();

        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("확정 후에 신고를 시도합니다 막혀야 합니다"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_TRANSACTION_STATUS")));
    }

    @Test
    @DisplayName("④ 사유 5자 → 400 VALIDATION_ERROR + fields[reason]")
    void 짧은_사유는_400() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("짧은사유다"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fields[0].name", is("reason")));
    }

    @Test
    @DisplayName("④ 증빙 4장 → 400 FILE_COUNT_EXCEEDED · 거래 상태는 그대로 PAID")
    void 파일_4장은_400() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("사진을 네 장 첨부해서 신고합니다"))
                        .file(evidence("a.png")).file(evidence("b.png"))
                        .file(evidence("c.png")).file(evidence("d.png"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("FILE_COUNT_EXCEEDED")));

        flushAndClear();

        mvc.perform(get("/api/transactions/{id}", txId).with(authentication(memberAuth(buyer))))
                .andExpect(jsonPath("$.data.status", is("PAID")));
    }

    @Test
    @DisplayName("④ 관리자 목록(T-018이 호출) — 접수하면 OPEN 목록에 뜨고, 강제 환불로 닫히면 빠진다")
    void 관리자_분쟁_목록() throws Exception {
        int before = disputeService.findOpenDisputes().size();

        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("관리자 목록에 뜨는지 확인하는 신고입니다"))
                        .with(authentication(memberAuth(buyer))))
                .andExpect(status().isCreated());
        flushAndClear();

        assertThat(disputeService.findOpenDisputes()).hasSize(before + 1);

        transactionService.forceRefund(txId);
        flushAndClear();

        assertThat(disputeService.findOpenDisputes()).hasSize(before);
    }

    @Test
    @DisplayName("(지킴) 비로그인 접수 → 401")
    void 비로그인은_401() throws Exception {
        mvc.perform(multipart("/api/transactions/{id}/disputes", txId)
                        .file(disputePart("로그인 없이 신고를 시도합니다 막혀야 합니다")))
                .andExpect(status().isUnauthorized());
    }
}
