package com.sealhackathon.api.calibration_sessions.entity;

import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Phiên hiệu chuẩn điểm — Coordinator/Head Judge yêu cầu các Judge cùng chấm
 * một bài mẫu để đồng bộ thang điểm trước Round chính.
 *
 * <p>GĐ3 (Sơ loại): gắn {@code track} — 1 phiên OPEN / track.
 * GĐ5 (Chung kết): {@code track} null — 1 phiên OPEN / round.
 */
@Entity
@Table(name = "calibration_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    /** Nullable — null cho CK (GĐ5); set cho Sơ loại theo bảng. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_submission_id")
    private Submission sampleSubmission;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalibrationStatus status = CalibrationStatus.OPEN;

    @Column(name = "target_score")
    private Float targetScore;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Builder.Default
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
