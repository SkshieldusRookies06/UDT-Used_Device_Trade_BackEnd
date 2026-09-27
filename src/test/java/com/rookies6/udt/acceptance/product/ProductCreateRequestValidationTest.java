package com.rookies6.udt.acceptance.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.dto.ProductCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 기능: 상품 등록 입력값 검증(SPEC §4.4 product 파트) — BE-C 0절 ①. DB·스프링 없이 DTO 어노테이션만 본다(1초).
 * 검증이 없으면 conditionGrade 'X'는 ConditionGrade.valueOf에서, priceKrw 누락은 NPE로 500이 난다.
 * HTTP로 400 + fields[]가 나가는지는 {@link ProductCreateValidationApiTest}.
 */
@DisplayName("[BE-C ①] 상품 등록 입력값 — 등급 S·A·B·C · 가격 필수·양수 · 길이 · 카테고리 숫자")
class ProductCreateRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static ProductCreateRequest request(String title, String description, Long price, String grade, String categoryId) {
        return new ProductCreateRequest(title, description, price, grade, categoryId);
    }

    private static ProductCreateRequest valid() {
        return request("아이폰 15", "상태 좋습니다", 500_000L, "A", "1");
    }

    private static Set<String> invalidFields(ProductCreateRequest r) {
        return validator.validate(r).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("(지킴) 올바른 요청은 위반 없음")
    void 올바른_요청은_통과한다() {
        assertThat(invalidFields(valid())).isEmpty();
    }

    @Test
    @DisplayName("① conditionGrade 'X' · 소문자 'a' · 누락 → conditionGrade 위반")
    void 등급은_SABC만() {
        assertThat(invalidFields(request("t", "d", 1000L, "X", "1"))).contains("conditionGrade");
        assertThat(invalidFields(request("t", "d", 1000L, "a", "1"))).contains("conditionGrade");
        assertThat(invalidFields(request("t", "d", 1000L, null, "1"))).contains("conditionGrade");
    }

    @Test
    @DisplayName("① priceKrw 누락 · 0 · 음수 → priceKrw 위반")
    void 가격은_필수이고_양수() {
        assertThat(invalidFields(request("t", "d", null, "A", "1"))).contains("priceKrw");
        assertThat(invalidFields(request("t", "d", 0L, "A", "1"))).contains("priceKrw");
        assertThat(invalidFields(request("t", "d", -1L, "A", "1"))).contains("priceKrw");
    }

    @Test
    @DisplayName("① title 101자 → title 위반 (100자까지)")
    void 제목은_100자까지() {
        assertThat(invalidFields(request("가".repeat(101), "d", 1000L, "A", "1"))).contains("title");
        assertThat(invalidFields(request("가".repeat(100), "d", 1000L, "A", "1"))).doesNotContain("title");
    }

    @Test
    @DisplayName("① description 빈 값 · 2001자 → description 위반")
    void 설명은_필수이고_2000자까지() {
        assertThat(invalidFields(request("t", " ", 1000L, "A", "1"))).contains("description");
        assertThat(invalidFields(request("t", "가".repeat(2001), 1000L, "A", "1"))).contains("description");
    }

    @Test
    @DisplayName("① categoryId 'abc' · 누락 → categoryId 위반")
    void 카테고리는_숫자() {
        assertThat(invalidFields(request("t", "d", 1000L, "A", "abc"))).contains("categoryId");
        assertThat(invalidFields(request("t", "d", 1000L, "A", null))).contains("categoryId");
    }
}
