package com.se194093.be.hackathons.repository;

import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.value_object.HackathonStatus;
import com.se194093.be.hackathons.value_object.Season;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HackathonRepository extends JpaRepository<Hackathon, Integer> {

    Optional<Hackathon> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameAndSeasonAndYear(String name, Season season, Integer year);

    @Query("""
            SELECT h FROM Hackathon h
            WHERE (:status IS NULL OR h.status = :status)
              AND (:year IS NULL OR h.year = :year)
              AND (:season IS NULL OR h.season = :season)
              AND (:q IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(h.slug) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Hackathon> search(
            @Param("status") HackathonStatus status,
            @Param("year") Integer year,
            @Param("season") Season season,
            @Param("q") String q,
            Pageable pageable
    );
}
