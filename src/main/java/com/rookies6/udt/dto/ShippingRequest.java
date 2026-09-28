package com.rookies6.udt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// SPEC 4.7: courier·trackingNo 둘 다 필수, trackingNo는 숫자 8~20자
public record ShippingRequest(
        @NotBlank String courier,
        @NotBlank @Pattern(regexp = "\\d{8,20}") String trackingNo) {
}