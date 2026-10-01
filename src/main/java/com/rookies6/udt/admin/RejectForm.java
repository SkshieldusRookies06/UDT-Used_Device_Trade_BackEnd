package com.rookies6.udt.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectForm(
        @NotBlank(message = "반려 사유를 입력해주세요")
        @Size(max = 200, message = "반려 사유는 200자 이내로 입력해주세요")
        String reason
) {}