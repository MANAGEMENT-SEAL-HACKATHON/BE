package com.sealhackathon.api.announcements.repository;

import com.sealhackathon.api.announcements.entity.AnnouncementView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnouncementViewRepository extends JpaRepository<AnnouncementView, Integer> {
    Optional<AnnouncementView> findByUserIdAndHackathonId(Integer userId, Integer hackathonId);
}
