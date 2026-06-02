package com.sealhackathon.api.appeals.repository;

import com.sealhackathon.api.appeals.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<Appeal, Integer> {
}
