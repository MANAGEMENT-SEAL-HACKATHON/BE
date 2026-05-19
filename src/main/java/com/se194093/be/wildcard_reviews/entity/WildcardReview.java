package com.se194093.be.wildcard_reviews.entity;

import com.se194093.be.rounds.entity.Round;
import com.se194093.be.teams.entity.Team;
import com.se194093.be.tracks.entity.Track;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-10] Phiếu đề xuất Wild Card cho 1 Team vào 1 Round.
 *
 * <p>Thêm {@code track_id} để theo dõi Wild Card xuất phát từ Track nào (đối với
 * Sơ loại nhiều Track). UNIQUE ({@code round_id}, {@code team_id}).
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

    @Column(name = "coordinator_approved")
    private Boolean coordinatorApproved;

    @Column(name = "coordinator_note", columnDefinition = "TEXT")
    private String coordinatorNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
