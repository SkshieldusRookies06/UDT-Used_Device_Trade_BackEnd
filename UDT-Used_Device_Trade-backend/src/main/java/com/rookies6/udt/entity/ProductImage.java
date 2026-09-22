package com.rookies6.udt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stored_name", nullable = false, length = 200)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 200)
    private String originalName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public ProductImage(String storedName, String originalName, int sortOrder) {
        this.storedName = storedName;
        this.originalName = originalName;
        this.sortOrder = sortOrder;
    }

    void assignTo(Product product) {
        this.product = product;
    }
}
