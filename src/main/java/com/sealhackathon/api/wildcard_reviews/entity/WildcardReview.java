package com.sealhackathon.api.wildcard_reviews.entity;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-10] Phiếu đề xuất Wild Card cho 1 Team vào 1 Round.
 *
 * <p>Thêm {@code track_id} để theo dõi Wild Card xuất phát từ Track nào (đối với
 * Sơ loại nhiều Track). UNIQUE ({@code round_id}, {@code team_id}).
 *
 * <p>Plan C: snapshot {@code submittedAt}/{@code proposalRank}/{@code systemProposed};
 * sau lock chỉ đổi qua Override (+ category/note).
 */
@Entity
@Table(
        name = "wildcard_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wr_round_team",
                        columnNames = {"round_id", "team_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /**
     * [BC-10] Track nguồn của Wild Card. Nullable — có thể null khi Wild Card
     * không gắn liền 1 Track cụ thể.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @Column(name = "avg_score")
    private Float avgScore;

    /** Snapshot thời điểm nộp bài sơ loại lúc đề xuất (Plan C sort tiebreak). */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Thứ hạng trong đề xuất hệ thống (1..slots) lúc build/confirm. */
    @Column(name = "proposal_rank")
    private Integer proposalRank;

    /** true nếu đội nằm trong top {@code slots} đề xuất hệ thống. */
    @Column(name = "system_proposed")
    private Boolean systemProposed;

    @Column(name = "coordinator_approved")
    private Boolean coordinatorApproved;

    @Column(name = "coordinator_note", columnDefinition = "TEXT")
    private String coordinatorNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Category lần override gần nhất (Plan C). */
    @Column(name = "override_reason_category", length = 40)
    private String overrideReasonCategory;

    @Column(name = "override_note", columnDefinition = "TEXT")
    private String overrideNote;

    @Builder.Default
    @Column(name = "is_override", nullable = false)
    private Boolean isOverride = false;
}
