package com.sealhackathon.api.wildcard_reviews.entity;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Plan C — lịch sử override vé vớt công khai cho Coordinator/Admin.
 */
@Entity
@Table(name = "wildcard_override_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardOverrideHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private WildcardReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "before_approved")
    private Boolean beforeApproved;

    @Column(name = "after_approved", nullable = false)
    private Boolean afterApproved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "by_user_id")
    private User byUser;

    @Column(name = "overridden_at", nullable = false)
    private LocalDateTime overriddenAt;
}
