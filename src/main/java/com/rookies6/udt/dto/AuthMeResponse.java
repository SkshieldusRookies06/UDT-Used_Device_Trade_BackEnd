package com.rookies6.udt.dto;

public record AuthMeResponse(String id, String email, String nickname, String role, Long balanceKrw) {
}