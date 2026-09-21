package com.rookies6.udt.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ProductDetailResponse(String id, String title, String description, long priceKrw,
                                    String conditionGrade, String status, String categoryName,
                                    String sellerId, String sellerNickname,
                                    List<ProductImageResponse> images, boolean wished, int wishCount,
                                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
