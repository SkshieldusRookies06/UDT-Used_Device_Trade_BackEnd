package com.rookies6.udt.controller;

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.dto.WishResponse;
import com.rookies6.udt.service.WishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{id}/wishes")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WishResponse> add(@PathVariable Long id) {

        // TODO -> T-005 이후 진행 가능
        Long userId = null;

        WishResponse response = wishService.addWish(userId, id);

        return ApiResponse.of(response, "찜 추가가 완료되었습니다");
    }

    @DeleteMapping
    public ApiResponse<WishResponse> remove(@PathVariable Long id) {

        // TODO -> T-005 이후 진행 가능
        Long userId = null;

        WishResponse response = wishService.removeWish(userId, id);

        return ApiResponse.of(response, "찜 해제가 완료되었습니다");
    }
}
