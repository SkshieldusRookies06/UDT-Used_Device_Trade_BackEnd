package com.rookies6.udt.acceptance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.rookies6.udt.dto.ShippingRequest;
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
 * 기능: 송장 입력값 검증(SPEC §4.7) — BE-D 0절 ⑥. DB·스프링 없이 DTO 어노테이션만 본다(1초).
 * HTTP로 400 + fields[]가 나가는지는 {@link ShippingApiTest}가 본다.
 */
@DisplayName("[BE-D ⑥] 송장 입력값 — trackingNo 숫자 8~20자 · courier 30자 이하")
class ShippingRequestValidationTest {

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

    private static Set<String> invalidFields(ShippingRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("⑥ trackingNo 'abc' → trackingNo 위반")
    void 문자_송장번호는_안_된다() {
        assertThat(invalidFields(new ShippingRequest("CJ대한통운", "abc"))).contains("trackingNo");
    }

    @Test
    @DisplayName("⑥ trackingNo 7자리 → 위반 (8자리부터)")
    void 일곱_자리는_짧다() {
        assertThat(invalidFields(new ShippingRequest("CJ대한통운", "1234567"))).contains("trackingNo");
    }

    @Test
    @DisplayName("⑥ trackingNo 21자리 → 위반 (20자리까지)")
    void 스물한_자리는_길다() {
        assertThat(invalidFields(new ShippingRequest("CJ대한통운", "123456789012345678901"))).contains("trackingNo");
    }

    @Test
    @DisplayName("⑥ courier 31자 → courier 위반")
    void 택배사_이름은_30자까지() {
        assertThat(invalidFields(new ShippingRequest("가".repeat(31), "123456789012"))).contains("courier");
    }

    @Test
    @DisplayName("(지킴) 올바른 값(숫자 8자리·20자리)은 통과 · 빈 값은 위반")
    void 올바른_값은_통과한다() {
        assertThat(invalidFields(new ShippingRequest("CJ대한통운", "12345678"))).isEmpty();
        assertThat(invalidFields(new ShippingRequest("CJ대한통운", "12345678901234567890"))).isEmpty();
        assertThat(invalidFields(new ShippingRequest(" ", " "))).contains("courier", "trackingNo");
    }
}
