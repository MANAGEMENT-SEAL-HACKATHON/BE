package com.sealhackathon.api.announcements.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hackathon_announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "hackathon_id", nullable = false)
    private Integer hackathonId;

    @Column(name = "round_id")
    private Integer roundId;

    @Column(name = "kind", nullable = false, length = 64)
    private String kind;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "soft_hidden", nullable = false)
    @Builder.Default
    private Boolean softHidden = false;
}
