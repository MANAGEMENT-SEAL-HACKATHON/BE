package com.se194093.be.rounds.entity;

import com.se194093.be.rounds.value_object.TiebreakRule;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> tracks(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    /**
     * Ví dụ:
     * - "Sơ loại"
     * - "Chung kết"
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "submission_open")
    private LocalDateTime submissionOpen;

    @Column(name = "submission_deadline", nullable = false)
    private LocalDateTime submissionDeadline;

    @Column(name = "coding_duration_hours")
    private Integer codingDurationHours;

    @Column(name = "problem_statement_url", columnDefinition = "TEXT")
    private String problemStatementUrl;

    @Column(name = "problem_released_at")
    private LocalDateTime problemReleasedAt;

    /**
     * NULL ở vòng chung kết
     */
    @Column(name = "top_n_advance")
    private Integer topNAdvance;

    /**
     * Per-round override
     * Chỉ có hiệu lực nếu hackathon.wildcard_enabled = TRUE
     */
    @Column(name = "wildcard_enabled", nullable = false)
    private Boolean wildcardEnabled = false;

    /**
     * [FIX-03]
     * Số đội tối thiểu vào vòng tiếp theo
     */
    @Column(name = "min_teams_final")
    private Integer minTeamsFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tiebreak_rule", length = 50)
    private TiebreakRule tiebreakRule = TiebreakRule.PENALTY_SCORE;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "scoring_locked", nullable = false)
    private Boolean scoringLocked = false;

    @Column(name = "scoring_locked_at")
    private LocalDateTime scoringLockedAt;

    // FK -> users(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoring_locked_by")
    private User scoringLockedBy;

    @Column(name = "force_locked", nullable = false)
    private Boolean forceLocked = false;

    @Column(name = "force_lock_reason", columnDefinition = "TEXT")
    private String forceLockReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
