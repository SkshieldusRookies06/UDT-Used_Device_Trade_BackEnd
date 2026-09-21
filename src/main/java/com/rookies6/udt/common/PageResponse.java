package com.rookies6.udt.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, PageMeta page) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements()));
    }

    public record PageMeta(int number, int size, long totalElements, int totalPages,
                           boolean first, boolean last, int numberOfElements) {
    }
}
