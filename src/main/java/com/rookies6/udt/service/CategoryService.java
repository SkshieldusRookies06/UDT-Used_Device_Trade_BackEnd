package com.rookies6.udt.service;

import com.rookies6.udt.dto.CategoryResponse;
import com.rookies6.udt.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("id"))
                .stream()
                .map(category -> new CategoryResponse(
                        String.valueOf(category.getId()), category.getName())
                )
                // 읽기 전용
                .toList();
    }
}
