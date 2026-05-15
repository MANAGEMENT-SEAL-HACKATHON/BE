package com.se194093.be.judge_assignments.entity;

import com.se194093.be.judge_assignments.value_object.JudgeAssignmentType;
import com.se194093.be.rounds.entity.Round;
import com.se194093.be.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "judge_assignments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"judge_id", "round_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> users(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    // FK -> rounds(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private JudgeAssignmentType assignmentType = JudgeAssignmentType.NORMAL;

    /**
     * Pending #6:
     * judge_weight FLOAT DEFAULT 1.0
     * Chờ BTC xác nhận INTERNAL vs EXTERNAL có trọng số khác nhau hay không
     */

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    // FK -> users(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
}
