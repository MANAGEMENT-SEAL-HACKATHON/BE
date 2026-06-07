package com.sealhackathon.api.events.entity;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.teams.entity.Team;
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

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "location", length = 300)
    private String location;

    @Column(name = "sequence_order")
    private Integer sequenceOrder;
}