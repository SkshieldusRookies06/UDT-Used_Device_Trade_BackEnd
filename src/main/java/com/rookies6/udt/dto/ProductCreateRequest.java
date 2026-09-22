package com.rookies6.udt.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductCreateRequest(
        @NotBlank
        String title,
        String description,
        Long priceKrw,
        String conditionGrade,
        String categoryId
) {
}
