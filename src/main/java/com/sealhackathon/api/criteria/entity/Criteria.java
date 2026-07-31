package com.sealhackathon.api.criteria.entity;

import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import jakarta.persistence.*;
import lombok.*;

/**
 * [BC-03] Criteria gắn XOR vào Track (Sơ loại) HOẶC Round (Chung kết).
 *
 * <p>Đúng 1 trong 2 FK NOT NULL, enforce bởi DB CHECK constraint + trigger
 * {@code trg_check_criteria_round_is_final}.
 *
 * <p>Xem chi tiết: {@code docs/db/schema-v3.0-mysql.md} §2 (criteria) và §5.8.
 */
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

    /**
     * [BC-03] FK XOR — Track (Sơ loại). Nullable=true; chỉ có khi {@code round} = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    /**
     * [BC-03] FK XOR — Round (Chung kết). Nullable=true; chỉ có khi {@code track} = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    /**
     * Tùy chọn — tham chiếu criterion gốc (cùng track/round). Clone từ track/round khác không set field này.
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
     * Giá trị trong khoảng (0, 1]. CHECK constraint ở DB.
     */
    @Column(name = "weight", nullable = false)
    private Float weight;

    @Builder.Default
    @Column(name = "max_score", nullable = false)
    private Integer maxScore = 10;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rubric_url", columnDefinition = "TEXT")
    private String rubricUrl;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    /**
     * Tiêu chí ưu tiên khi tie-break (tối đa 1 / track hoặc 1 / final round; không dùng cho PENALTY).
     */
    @Builder.Default
    @Column(name = "is_tiebreaker_priority", nullable = false)
    private Boolean isTiebreakerPriority = false;
}
