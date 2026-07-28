package com.sealhackathon.api.criteria.repository;

import com.sealhackathon.api.criteria.entity.CriteriaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CriteriaTemplateRepository extends JpaRepository<CriteriaTemplate, Integer> {
    List<CriteriaTemplate> findAllByOrderByIsDefaultDescNameAsc();
    Optional<CriteriaTemplate> findFirstByIsDefaultTrue();
}
