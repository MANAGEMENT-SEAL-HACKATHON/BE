package com.sealhackathon.api.mentors.entity;

import com.sealhackathon.api.common.value_object.AssignmentResponseStatus;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mentor_assignments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"mentor_id", "track_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> users(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    // FK -> tracks(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    // FK -> users(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    /** Default ACCEPTED — must NOT be PENDING or existing gates break. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 20)
    private AssignmentResponseStatus responseStatus = AssignmentResponseStatus.ACCEPTED;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "decline_reason", length = 1000)
    private String declineReason;
}
