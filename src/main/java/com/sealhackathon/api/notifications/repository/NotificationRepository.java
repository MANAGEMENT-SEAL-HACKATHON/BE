package com.sealhackathon.api.notifications.repository;

import com.sealhackathon.api.notifications.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByUserIdOrderBySentAtDesc(Integer userId, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalse(Integer userId);

    long countByUserIdAndIsReadFalse(Integer userId);

    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, Integer referenceId);
}
