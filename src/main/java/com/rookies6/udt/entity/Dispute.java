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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "disputes", uniqueConstraints = @UniqueConstraint(
        name = "uk_disputes_transaction", columnNames = "transaction_id"))
public class Dispute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DisputeFile> files = new ArrayList<>();

    @Builder
    public Dispute(Transaction transaction, User reporter, String reason) {
        this.transaction = transaction;
        this.reporter = reporter;
        this.reason = reason;
        this.status = DisputeStatus.OPEN;
    }

    public void addFile(DisputeFile file) {
        files.add(file);
        file.assignTo(this);
    }

    public void resolve(String adminMemo, OffsetDateTime at) {
        this.status = DisputeStatus.RESOLVED;
        this.adminMemo = adminMemo;
        this.resolvedAt = at;
    }
}
