package com.sealhackathon.api.showcase.repository;

import com.sealhackathon.api.showcase.entity.HallOfFameEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HallOfFameEntryRepository extends JpaRepository<HallOfFameEntry, Integer> {

    Optional<HallOfFameEntry> findByHackathonId(Integer hackathonId);

    boolean existsByHackathonId(Integer hackathonId);

    List<HallOfFameEntry> findAllByOrderByYearDescSeasonAsc();

    List<HallOfFameEntry> findByYearOrderBySeasonAsc(Integer year);
}
