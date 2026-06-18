package com.sealhackathon.api.events.entity;

import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Judge xác nhận đã chấm xong một bài trong phiên PRESENTING (multi-judge track). */
@Entity
@Table(
        name = "judge_submission_scoring_confirmations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id", "judge_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeSubmissionScoringConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;
}
