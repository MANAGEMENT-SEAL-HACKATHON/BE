package com.sealhackathon.api.criteria.entity;

import com.sealhackathon.api.criteria.value_object.CriteriaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criteria_template_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriteriaTemplateItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CriteriaTemplate template;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CriteriaType type;

    @Column(nullable = false)
    private Float weight;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
