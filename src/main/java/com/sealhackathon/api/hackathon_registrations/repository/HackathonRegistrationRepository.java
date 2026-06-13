package com.sealhackathon.api.hackathon_registrations.repository;

import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HackathonRegistrationRepository extends JpaRepository<HackathonRegistration, Integer> {

    // Kiểm tra nhanh xem User đã đăng ký Hackathon này chưa (Phục vụ Guard FR-U-06)
    boolean existsByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    void deleteByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    Optional<HackathonRegistration> findByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    long countByHackathon_Id(Integer hackathonId);
}