package com.rookies6.udt.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingRequest(@NotBlank String courier, @NotBlank String trackingNo) {
}