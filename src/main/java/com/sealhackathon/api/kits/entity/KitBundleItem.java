package com.sealhackathon.api.kits.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "kit_bundle_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_kit_bundle_item",
                columnNames = {"bundle_id", "kit_item_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitBundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", nullable = false)
    private KitBundle bundle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kit_item_id", nullable = false)
    private KitItem kitItem;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
}
