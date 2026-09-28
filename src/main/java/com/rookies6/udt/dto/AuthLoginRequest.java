package com.rookies6.udt.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(@NotBlank String email, @NotBlank String password) {
}