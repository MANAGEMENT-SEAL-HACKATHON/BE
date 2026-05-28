package com.sealhackathon.api.users.entity;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    /**
     * NULL nếu đăng nhập OAuth
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    /**
     * [FIX-01]
     * INTERNAL -> mã SV FPT
     * EXTERNAL -> mã ngoài trường
     */
    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Column(name = "is_temp_account", nullable = false)
    private Boolean isTempAccount = false;

    /**
     * [FIX-02] Cờ "Trưởng khoa" — cho phép ngoại lệ làm Judge Chung kết
     * dù là INTERNAL, miễn KHÔNG đồng thời là Mentor.
     * Trigger {@code trg_check_mentor_judge_conflict_ins} (docs §5.4) sử dụng cờ này.
     */
    @Builder.Default
    @Column(name = "is_dept_head", nullable = false)
    private Boolean isDeptHead = false;

    /**
     * Judge khách: bắt buộc đổi MK sau lần đăng nhập đầu (hoặc sau resend).
     */
    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // FK -> chapters(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * Trường học / công ty với EXTERNAL
     */
    @Column(name = "institution", length = 300)
    private String institution;

    /**
     * Ảnh thẻ sinh viên phục vụ Coordinator đối chiếu thông tin trước khi duyệt.
     */
    @Column(name = "student_card_image_path", columnDefinition = "TEXT")
    private String studentCardImagePath;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
