package com.sealhackathon.api.hackathon_registrations.repository;

import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistrationWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HackathonRegistrationWithdrawalRepository extends JpaRepository<HackathonRegistrationWithdrawal, Integer> {

    boolean existsByHackathon_IdAndUser_Id(Integer hackathonId, Integer userId);

    void deleteByHackathon_Id(Integer hackathonId);
}
