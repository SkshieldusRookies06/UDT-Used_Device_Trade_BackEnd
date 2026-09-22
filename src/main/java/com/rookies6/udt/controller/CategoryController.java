package com.rookies6.udt.controller;

// TODO(T-006) BE-C — 하드코딩 껍데기. CategoryService 연결 후 교체한다.

import com.rookies6.udt.common.ApiResponse;
import com.rookies6.udt.dto.CategoryResponse;
import java.util.List;

import com.rookies6.udt.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
//        List<CategoryResponse> categories = List.of(
//                new CategoryResponse("1", "노트북"),
//                new CategoryResponse("2", "스마트폰"),
//                new CategoryResponse("3", "태블릿"),
//                new CategoryResponse("4", "이어폰"),
//                new CategoryResponse("5", "기타"));

        List<CategoryResponse> categories = categoryService.getCategories();
        return ApiResponse.of(categories, "카테고리 조회가 완료되었습니다");
    }
}
