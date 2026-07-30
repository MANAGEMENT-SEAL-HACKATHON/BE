package com.sealhackathon.api.appeals.entity;

import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** FR-U-30 — khiếu nại DQ trong cửa sổ nghỉ sau công bố sơ loại. */
@Entity
@Table(name = "appeals", uniqueConstraints = {
        @UniqueConstraint(name = "uq_appeals_team_round", columnNames = {"team_id", "round_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** Legacy single evidence URL — prefer {@link #evidences}. */
    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppealStatus status = AppealStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "decision_note", columnDefinition = "TEXT")
    private String decisionNote;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "appeal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<AppealEvidence> evidences = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
