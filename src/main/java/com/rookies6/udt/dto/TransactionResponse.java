package com.rookies6.udt.dto;

import com.rookies6.udt.entity.Transaction;

import java.time.OffsetDateTime;

public record TransactionResponse(String id, String productId, String buyerId, String sellerId,
                                  long amountKrw, String status,
                                  String courier, String trackingNo,
                                  OffsetDateTime confirmedAt, OffsetDateTime createdAt) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                String.valueOf(tx.getId()),
                String.valueOf(tx.getProduct().getId()),
                String.valueOf(tx.getBuyer().getId()),
                String.valueOf(tx.getProduct().getSeller().getId()),
                tx.getAmountKrw(),
                tx.getStatus().name(),
                tx.getCourier(),
                tx.getTrackingNo(),
                tx.getConfirmedAt(),
                tx.getCreatedAt()
        );
    }
}