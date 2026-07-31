package com.sealhackathon.api.kits.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "kit_stocks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_kit_stock_item_fit_size",
                columnNames = {"kit_item_id", "fit_key", "size_key"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kit_item_id", nullable = false)
    private KitItem kitItem;

    /** Display fit (nullable when item is not SHIRT). */
    @Column(name = "fit", length = 20)
    private String fit;

    /**
     * Unique companion for nullable fit — empty string when fit is null
     * (MySQL UNIQUE allows multiple NULLs).
     */
    @Builder.Default
    @Column(name = "fit_key", nullable = false, length = 20)
    private String fitKey = "";

    /** Display size (nullable when item has no size). */
    @Column(name = "size", length = 10)
    private String size;

    /**
     * Unique companion for nullable size — empty string when size is null
     * (MySQL UNIQUE allows multiple NULLs).
     */
    @Builder.Default
    @Column(name = "size_key", nullable = false, length = 10)
    private String sizeKey = "";

    @Builder.Default
    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal = 0;

    @Builder.Default
    @Column(name = "quantity_issued", nullable = false)
    private Integer quantityIssued = 0;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    @PreUpdate
    void syncSizeKey() {
        this.sizeKey = size == null ? "" : size;
        this.fitKey = fit == null ? "" : fit;
    }

    public int remaining() {
        int total = quantityTotal == null ? 0 : quantityTotal;
        int issued = quantityIssued == null ? 0 : quantityIssued;
        return Math.max(0, total - issued);
    }
}
