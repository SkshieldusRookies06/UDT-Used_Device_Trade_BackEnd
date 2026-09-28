package com.rookies6.udt.dto;

import com.rookies6.udt.entity.Dispute;
import com.rookies6.udt.entity.Transaction;

import java.time.OffsetDateTime;

public record TransactionResponse(String id, String productId, String productTitle,
                                  String buyerId, String sellerId,
                                  String buyerNickname, String sellerNickname,
                                  long amountKrw, String status,
                                  String courier, String trackingNo,
                                  OffsetDateTime createdAt, OffsetDateTime confirmedAt,
                                  DisputeResponse dispute) {

    public static TransactionResponse from(Transaction tx) {
        return from(tx, null);
    }

    public static TransactionResponse from(Transaction tx, Dispute dispute) {
        return new TransactionResponse(
                String.valueOf(tx.getId()),
                String.valueOf(tx.getProduct().getId()),
                tx.getProduct().getTitle(),
                String.valueOf(tx.getBuyer().getId()),
                String.valueOf(tx.getProduct().getSeller().getId()),
                tx.getBuyer().getNickname(),
                tx.getProduct().getSeller().getNickname(),
                tx.getAmountKrw(),
                tx.getStatus().name(),
                tx.getCourier(),
                tx.getTrackingNo(),
                tx.getCreatedAt(),
                tx.getConfirmedAt(),
                dispute == null ? null : DisputeResponse.from(dispute));
    }
}