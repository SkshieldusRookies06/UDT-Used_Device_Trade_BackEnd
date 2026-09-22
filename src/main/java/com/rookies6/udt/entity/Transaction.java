package com.rookies6.udt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transactions", indexes = @Index(
        name = "idx_transactions_product", columnList = "product_id"))
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "amount_krw", nullable = false)
    private Long amountKrw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 30)
    private String courier;

    @Column(name = "tracking_no", length = 30)
    private String trackingNo;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Builder
    public Transaction(Product product, User buyer, Long amountKrw) {
        this.product = product;
        this.buyer = buyer;
        this.amountKrw = amountKrw;
        this.status = TransactionStatus.PAID;
    }

    public void registerShipping(String courier, String trackingNo) {
        this.courier = courier;
        this.trackingNo = trackingNo;
        this.status = TransactionStatus.SHIPPING;
    }

    public void confirm(OffsetDateTime at) {
        this.status = TransactionStatus.CONFIRMED;
        this.confirmedAt = at;
    }

    public void changeStatus(TransactionStatus next) {
        this.status = next;
    }
}
