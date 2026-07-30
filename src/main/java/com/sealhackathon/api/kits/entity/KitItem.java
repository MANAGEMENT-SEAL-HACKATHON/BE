package com.sealhackathon.api.kits.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.kits.value_object.KitItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kit_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private KitItemType type;

    @Builder.Default
    @Column(name = "has_size", nullable = false)
    private Boolean hasSize = false;
}
