package com.sealhackathon.api.judge_assignments.entity;

import com.sealhackathon.api.judge_assignments.value_object.JudgeAssignmentType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * [BC-07] JudgeAssignment dùng XOR FK — gắn vào Track (Sơ loại) HOẶC Round (Chung kết).
 *
 * <p>Đúng 1 trong 2 FK NOT NULL. UNIQUE partial mô phỏng bằng generated columns
 * {@code track_uk}, {@code round_uk} ở DB layer (KHÔNG khai báo trong entity).
 *
 * <p>Trigger {@code trg_check_mentor_judge_conflict_ins/upd} ({@code docs/db/schema-v3.0-mysql.md} §5.4)
 * enforce:
 * <ul>
 *   <li>Sơ loại: Judge không được là Mentor cùng track.</li>
 *   <li>Chung kết: phải {@code FINAL_EXTERNAL} + 100% EXTERNAL (ngoại lệ Dept head không-mentor).</li>
 * </ul>
 */
@Entity
@Table(name = "judge_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    /**
     * [BC-07] FK XOR — Track (Sơ loại/Bán kết). Nullable=true; chỉ có khi {@code round} = null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    /**
     * [BC-07] FK XOR — Round (Chung kết). Nullable=true; chỉ có khi {@code track} = null.
     * Khi NOT NULL, {@code assignmentType} BẮT BUỘC = {@code FINAL_EXTERNAL}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private JudgeAssignmentType assignmentType = JudgeAssignmentType.NORMAL;

    @Builder.Default
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
}
