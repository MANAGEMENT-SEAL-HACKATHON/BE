package com.sealhackathon.api.invitations.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    /**
     * Role được mời:
     * COORDINATOR / JUDGE / MENTOR / STUDENT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * NULL = mời toàn hệ thống
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id")
    private Hackathon hackathon;

    // FK -> users(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /**
     * Kết quả lần gửi email gần nhất. {@code false} = SMTP fail → cho phép resend ngay
     * dù token còn hạn. {@code null} coi như đã gửi thành công (row cũ).
     */
    @Column(name = "last_token_sent")
    private Boolean lastTokenSent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
