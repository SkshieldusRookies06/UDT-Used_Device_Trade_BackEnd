package com.rookies6.udt.dto;

import jakarta.validation.constraints.*;

public record ProductCreateRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        String title,

        @NotBlank(message = "설명은 필수입니다")
        @Size(max = 2000, message = "설명은 2000자 이하여야 합니다")
        String description,

        @NotNull(message = "가격은 필수입니다")
        @Positive(message = "가격은 0보다 커야 합니다")
        Long priceKrw,

        @NotBlank(message = "상태 등급은 필수입니다")
        @Pattern(regexp = "S|A|B|C", message = "상태 등급은 S, A, B, C 중 하나여야 합니다")
        String conditionGrade,

        @NotBlank(message = "카테고리는 필수입니다")
        @Pattern(regexp = "\\d{1,18}", message = "카테고리 값이 올바르지 않습니다")
        String categoryId
) {
}
