package com.rookies6.udt.service;

import com.rookies6.udt.common.BusinessException;
import com.rookies6.udt.common.ErrorCode;
import com.rookies6.udt.dto.AuthLoginRequest;
import com.rookies6.udt.dto.AuthLoginResponse;
import com.rookies6.udt.dto.AuthMeResponse;
import com.rookies6.udt.dto.AuthSignupRequest;
import com.rookies6.udt.dto.AuthSignupResponse;
import com.rookies6.udt.entity.Role;
import com.rookies6.udt.entity.User;
import com.rookies6.udt.repository.UserRepository;
import com.rookies6.udt.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthSignupResponse signup(AuthSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User saved = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(Role.MEMBER)
                .balanceKrw(0L)
                .build());

        return new AuthSignupResponse(String.valueOf(saved.getId()), saved.getEmail(), saved.getNickname());
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getRole().name(), user.getNickname());
        return new AuthLoginResponse(token, new AuthLoginResponse.UserSummary(
                String.valueOf(user.getId()), user.getNickname(), user.getRole().name(), user.getBalanceKrw()));
    }

    public AuthMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new AuthMeResponse(String.valueOf(user.getId()), user.getEmail(), user.getNickname(),
                user.getRole().name(), user.getBalanceKrw());
    }
}