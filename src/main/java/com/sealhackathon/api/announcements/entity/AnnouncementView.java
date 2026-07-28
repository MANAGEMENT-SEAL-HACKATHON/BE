package com.sealhackathon.api.announcements.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "hackathon_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "hackathon_id", nullable = false)
    private Integer hackathonId;

    @Column(name = "last_viewed_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastViewedAt = LocalDateTime.now();
}
