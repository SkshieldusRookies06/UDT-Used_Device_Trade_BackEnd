package com.rookies6.udt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthSignupRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다")
        @Pattern(regexp = "^[\\x21-\\x7E]+$", message = "비밀번호는 영문·숫자·특수문자만 사용할 수 있습니다(공백·한글 불가)")
        String password,
        @NotBlank @Size(max = 30) String nickname) {
}