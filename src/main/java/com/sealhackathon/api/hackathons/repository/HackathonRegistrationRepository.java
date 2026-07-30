package com.sealhackathon.api.hackathons.repository;

import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HackathonRegistrationRepository extends JpaRepository<HackathonRegistration, Integer> {

    // Kiểm tra nhanh xem User đã đăng ký Hackathon này chưa (Phục vụ Guard FR-U-06)
    java.util.List<HackathonRegistration> findAllByHackathon_Id(Integer hackathonId);

    java.util.List<HackathonRegistration> findAllByUser_Id(Integer userId);

    boolean existsByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    void deleteByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    Optional<HackathonRegistration> findByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    long countByHackathon_Id(Integer hackathonId);

    void deleteByHackathon_Id(Integer hackathonId);

    boolean existsByUser_IdAndHackathon_Status(Integer userId, HackathonStatus status);

    boolean existsByUser_IdAndHackathon_StatusAndHackathon_IdNot(
            Integer userId, HackathonStatus status, Integer hackathonId);
}