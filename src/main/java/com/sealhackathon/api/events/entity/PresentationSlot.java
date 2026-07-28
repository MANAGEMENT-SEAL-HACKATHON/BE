package com.sealhackathon.api.events.entity;

import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng quản lý Lịch thuyết trình cụ thể cho từng đội (FR-M-12, FR-J-12).
 */
@Entity
@Table(name = "presentation_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "timer_phase", length = 20)
    private PresentationTimerPhase timerPhase = PresentationTimerPhase.IDLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "timer_phase_before_pause", length = 20)
    private PresentationTimerPhase timerPhaseBeforePause;

    @Column(name = "presentation_started_at")
    private LocalDateTime presentationStartedAt;

    @Column(name = "qa_started_at")
    private LocalDateTime qaStartedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Builder.Default
    @Column(name = "paused_accumulated_seconds", nullable = false)
    private Integer pausedAccumulatedSeconds = 0;

    /**
     * true = Q&A kết thúc sớm (cần đủ GK chốt trước khi next, trừ force-ack).
     * false = hết giờ tự nhiên (next ghi nhận điểm tới đâu, thiếu cũng được).
     * null = chưa kết thúc Q&A / slot mới.
     */
    @Column(name = "qa_ended_early")
    private Boolean qaEndedEarly;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "location", length = 300)
    private String location;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", length = 20)
    private PresentationQueueStatus queueStatus = PresentationQueueStatus.WAITING;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}