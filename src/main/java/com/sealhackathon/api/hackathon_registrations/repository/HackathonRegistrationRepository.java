package com.sealhackathon.api.hackathon_registrations.repository;

import com.sealhackathon.api.hackathon_registrations.entity.HackathonRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HackathonRegistrationRepository extends JpaRepository<HackathonRegistration, Integer> {
}
