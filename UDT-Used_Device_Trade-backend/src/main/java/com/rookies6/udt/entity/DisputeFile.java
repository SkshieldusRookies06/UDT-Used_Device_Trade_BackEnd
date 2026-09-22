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
@Table(name = "dispute_files")
public class DisputeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @Column(name = "stored_name", nullable = false, length = 200)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 200)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Builder
    public DisputeFile(String storedName, String originalName, long sizeBytes) {
        this.storedName = storedName;
        this.originalName = originalName;
        this.sizeBytes = sizeBytes;
    }

    void assignTo(Dispute dispute) {
        this.dispute = dispute;
    }
}
