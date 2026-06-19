package com.sealhackathon.api.hackathon_registrations.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Ghi nhận sinh viên đã hủy đăng ký — mỗi người chỉ được hủy một lần / giải. */
@Entity
@Table(name = "hackathon_registration_withdrawals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hackathon_reg_withdrawal_user",
                columnNames = {"hackathon_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonRegistrationWithdrawal {

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
    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt = LocalDateTime.now();
}
