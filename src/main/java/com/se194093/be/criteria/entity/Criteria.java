package com.se194093.be.criteria.entity;

import com.se194093.be.criteria.value_object.CriteriaType;
import com.se194093.be.rounds.entity.Round;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Criteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> rounds(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    /**
     * Kế thừa criteria từ kỳ trước
     * FK tự tham chiếu
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_criteria_id")
    private Criteria sourceCriteria;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CriteriaType type;

    /**
     * weight > 0 && weight <= 1
     */
    @Column(name = "weight", nullable = false)
    private Float weight;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore = 10;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rubric_url", columnDefinition = "TEXT")
    private String rubricUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}