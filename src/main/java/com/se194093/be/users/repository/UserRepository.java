package com.se194093.be.users.repository;

import com.se194093.be.users.entity.User;
import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
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

    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    /**
     * Filter Judge khách mời (Temp Judge): role=JUDGE, user_type=EXTERNAL, is_temp_account=TRUE.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role = com.se194093.be.users.value_object.UserRole.JUDGE
              AND u.userType = com.se194093.be.users.value_object.UserType.EXTERNAL
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
}
