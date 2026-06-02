package com.sealhackathon.api.certificates.entity;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** FR-U-29 — giấy chứng nhận tham gia (O-NEW-4). */
@Entity
@Table(name = "certificates",
        uniqueConstraints = @UniqueConstraint(name = "uk_cert_user_hackathon", columnNames = {"user_id", "hackathon_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Hackathon hackathon;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Builder.Default
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();
}
