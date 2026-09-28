package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.dto.AuthLoginRequest;
import com.rookies6.udt.dto.AuthLoginResponse;
import com.rookies6.udt.dto.AuthMeResponse;
import com.rookies6.udt.dto.AuthSignupRequest;
import com.rookies6.udt.dto.AuthSignupResponse;
import com.rookies6.udt.security.CurrentUser;
import com.rookies6.udt.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/signup")
    public ResponseEntity<ApiResponse<AuthSignupResponse>> signup(@Valid @RequestBody AuthSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(authService.signup(request), "회원가입이 완료되었습니다"));
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.of(authService.login(request), "로그인이 완료되었습니다");
    }

    @GetMapping("/api/me")
    public ApiResponse<AuthMeResponse> me() {
        return ApiResponse.of(authService.getMe(CurrentUser.id()), "내 정보 조회가 완료되었습니다");
    }
}