package com.sealhackathon.api.users.repository;

import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentCode(String studentCode);

    boolean existsByStudentCodeAndIdNot(String studentCode, Integer id);

    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    /**
     * Filter Judge khách mời (Temp Judge): role=JUDGE, user_type=EXTERNAL, is_temp_account=TRUE.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role = com.sealhackathon.api.users.value_object.UserRole.JUDGE
              AND u.userType = com.sealhackathon.api.users.value_object.UserType.EXTERNAL
              AND u.isTempAccount = TRUE
              AND (:institution IS NULL OR LOWER(u.institution) LIKE LOWER(CONCAT('%', :institution, '%')))
              AND (:q IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> searchTempJudges(@Param("institution") String institution,
                                @Param("q") String q,
                                Pageable pageable);

    /**
     * Đếm bài đang APPROVED toàn hệ thống — dùng trong notification fan-out khi chuyển ONGOING.
     */
    long countByStatus(UserStatus status);

    /**
     * Lấy mọi user theo status — dùng cho fan-out notification HACKATHON_OPEN khi DRAFT→ONGOING.
     */
    java.util.List<User> findAllByStatus(UserStatus status);

    java.util.List<User> findByStatusAndUserTypeAndEmailVerifiedAtIsNotNull(
            UserStatus status, com.sealhackathon.api.users.value_object.UserType userType);

    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR u.role = :role)
              AND (:personnelOnly IS NULL OR :personnelOnly = FALSE
                   OR u.role IN (com.sealhackathon.api.users.value_object.UserRole.MENTOR,
                                 com.sealhackathon.api.users.value_object.UserRole.JUDGE))
              AND (:userType IS NULL OR u.userType = :userType)
              AND (:q IS NULL OR :q = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> searchAdmin(@Param("status") UserStatus status,
                           @Param("role") UserRole role,
                           @Param("personnelOnly") Boolean personnelOnly,
                           @Param("userType") UserType userType,
                           @Param("q") String q,
                           Pageable pageable);
}
