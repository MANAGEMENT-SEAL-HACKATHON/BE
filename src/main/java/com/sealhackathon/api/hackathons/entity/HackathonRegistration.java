package com.sealhackathon.api.hackathons.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** FR-U-06 — đăng ký tham gia hackathon (student). */
@Entity
@Table(name = "hackathon_registrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_hackathon_reg_user", columnNames = {"hackathon_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();
}
