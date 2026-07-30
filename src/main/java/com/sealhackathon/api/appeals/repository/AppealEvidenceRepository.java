package com.sealhackathon.api.appeals.repository;

import com.sealhackathon.api.appeals.entity.AppealEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealEvidenceRepository extends JpaRepository<AppealEvidence, Integer> {

    List<AppealEvidence> findByAppeal_IdOrderByDisplayOrderAscIdAsc(Integer appealId);
}
