package com.rookies6.udt.dto;

import com.rookies6.udt.entity.Dispute;

import java.time.OffsetDateTime;
import java.util.List;

public record DisputeResponse(String id, String transactionId, String status, String reason,
                              List<FileItem> files, OffsetDateTime createdAt) {

    public record FileItem(String id, String originalName) {
    }

    public static DisputeResponse from(Dispute dispute) {
        return new DisputeResponse(
                String.valueOf(dispute.getId()),
                String.valueOf(dispute.getTransaction().getId()),
                dispute.getStatus().name(),
                dispute.getReason(),
                dispute.getFiles().stream()
                        .map(f -> new FileItem(String.valueOf(f.getId()), f.getOriginalName()))
                        .toList(),
                dispute.getCreatedAt());
    }
}