package com.rookies6.udt.dto;

import java.time.OffsetDateTime;

public record ProductSummaryResponse(String id, String title, long priceKrw, String conditionGrade,
                                     String status, String categoryName, String sellerNickname,
                                     String thumbnailUrl, int wishCount, OffsetDateTime createdAt) {
}
