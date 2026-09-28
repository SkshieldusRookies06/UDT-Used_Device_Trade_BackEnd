package com.rookies6.udt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisputeCreateRequest(
        @NotBlank @Size(min = 10, max = 500, message = "사유는 10~500자여야 합니다") String reason) {
}
