package com.rookies6.udt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingRequest(
        @NotBlank @Size(max = 30) String courier,
        @NotBlank @Pattern(regexp = "\\d{8,20}", message = "송장번호는 숫자 8~20자리입니다") String trackingNo) {
}