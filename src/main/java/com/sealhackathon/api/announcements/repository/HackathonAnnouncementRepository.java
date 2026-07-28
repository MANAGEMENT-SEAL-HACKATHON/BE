package com.sealhackathon.api.announcements.repository;

import com.sealhackathon.api.announcements.entity.HackathonAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HackathonAnnouncementRepository extends JpaRepository<HackathonAnnouncement, Integer> {
    List<HackathonAnnouncement> findByHackathonIdAndSoftHiddenFalseOrderByCreatedAtDesc(Integer hackathonId);
    List<HackathonAnnouncement> findByHackathonIdOrderByCreatedAtDesc(Integer hackathonId);
}
