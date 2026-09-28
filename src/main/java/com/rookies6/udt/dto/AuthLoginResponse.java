package com.rookies6.udt.dto;

public record AuthLoginResponse(String accessToken, UserSummary user) {

    public record UserSummary(String id, String nickname, String role, Long balanceKrw) {
    }
}