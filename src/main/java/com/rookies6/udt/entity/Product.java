package com.rookies6.udt.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products", indexes = {
        @Index(name = "idx_products_status_created", columnList = "status, created_at"),
        @Index(name = "idx_products_title", columnList = "title")
})
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "price_krw", nullable = false)
    private Long priceKrw;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_grade", nullable = false, length = 5)
    private ConditionGrade conditionGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @OrderBy("sortOrder asc")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @Builder
    public Product(User seller, Category category, String title, String description,
                   Long priceKrw, ConditionGrade conditionGrade) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.priceKrw = priceKrw;
        this.conditionGrade = conditionGrade;
        this.status = ProductStatus.INSPECTING;
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.assignTo(this);
    }

    public void changeStatus(ProductStatus next) {
        this.status = next;
    }

    public void reject(String reason) {
        this.status = ProductStatus.REJECTED;
        this.rejectReason = reason;
    }
}
