package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.acceptance.AcceptanceSupport;
import com.rookies6.udt.acceptance.AcceptanceTest;
import com.rookies6.udt.dto.TransactionResponse;
import com.rookies6.udt.entity.Dispute;
import com.rookies6.udt.entity.Product;
import com.rookies6.udt.entity.Transaction;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.DisputeRepository;
import com.rookies6.udt.repository.TransactionRepository;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기능: 응답 DTO 모양(SPEC §4.5 Transaction 객체 · §4.8 분쟁 객체) — BE-D 0절 ⑦·⑧.
 * 컨트롤러와 무관하게 DTO만 본다. 아직 없는 필드·클래스를 직접 부르면 main이 컴파일되지 않으므로
 * 이름으로(리플렉션) 찾는다 — 없으면 red, 만들면 green.
 */
@AcceptanceTest
@DisplayName("[BE-D ⑦⑧] 응답 DTO — TransactionResponse 필드 · DisputeResponse 신설")
class TransactionResponseShapeTest extends AcceptanceSupport {

    private static final String DISPUTE_RESPONSE = "com.rookies6.udt.dto.DisputeResponse";

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private DisputeRepository disputeRepository;

    private User seller;
    private User buyer;
    private Long txId;

    @BeforeEach
    void setUp() {
        seller = member("rs-seller", 0L);
        buyer = member("rs-buyer", 1_000_000L);
        Product product = onSaleProduct(seller, 300_000L);
        flushAndClear();
        txId = paidTransaction(buyer, product);
    }

    @Test
    @DisplayName("⑧ TransactionResponse에 productTitle · buyerNickname · sellerNickname · dispute가 있고 값이 채워진다")
    void 거래_응답에_상품명과_닉네임이_있다() throws Exception {
        TransactionResponse response = TransactionResponse.from(transactionRepository.findById(txId).orElseThrow());

        assertThat(componentNames(TransactionResponse.class))
                .contains("id", "productId", "productTitle", "buyerId", "sellerId", "buyerNickname", "sellerNickname",
                        "amountKrw", "status", "courier", "trackingNo", "createdAt", "confirmedAt", "dispute");
        assertThat(read(response, "productTitle")).isEqualTo(PRODUCT_TITLE);
        assertThat(read(response, "buyerNickname")).isEqualTo(buyer.getNickname());
        assertThat(read(response, "sellerNickname")).isEqualTo(seller.getNickname());
        assertThat(read(response, "dispute")).as("분쟁 없는 거래는 dispute = null").isNull();
    }

    @Test
    @DisplayName("⑧ TransactionResponse.from(Transaction, Dispute)가 있다 — 거래 상세·강제 처리 응답에 분쟁을 붙인다")
    void 분쟁을_붙이는_from이_있다() throws Exception {
        assertThat(TransactionResponse.class.getMethod("from", Transaction.class, Dispute.class)).isNotNull();
    }

    @Test
    @DisplayName("⑦ DisputeResponse(id, transactionId, status, reason, files, createdAt)가 있고 from(Dispute)로 만든다")
    void 분쟁_응답_DTO가_있다() throws Exception {
        Class<?> type = Class.forName(DISPUTE_RESPONSE);
        assertThat(componentNames(type)).containsExactly("id", "transactionId", "status", "reason", "files", "createdAt");

        transactionService.markDisputed(buyer.getId(), txId);
        Dispute dispute = disputeRepository.saveAndFlush(Dispute.builder()
                .transaction(transactionRepository.findById(txId).orElseThrow())
                .reporter(userRepository.findById(buyer.getId()).orElseThrow())
                .reason("박스만 왔습니다").build());

        Object response = type.getMethod("from", Dispute.class).invoke(null, dispute);
        assertThat(read(response, "id")).isEqualTo(String.valueOf(dispute.getId()));
        assertThat(read(response, "transactionId")).isEqualTo(String.valueOf(txId));
        assertThat(read(response, "status")).isEqualTo("OPEN");
        assertThat(read(response, "reason")).isEqualTo("박스만 왔습니다");
        assertThat((List<?>) read(response, "files")).isEmpty();
    }

    private static List<String> componentNames(Class<?> type) {
        assertThat(type.isRecord()).as(type.getSimpleName() + "는 record").isTrue();
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

    private static Object read(Object record, String component) throws Exception {
        return record.getClass().getMethod(component).invoke(record);
    }
}
