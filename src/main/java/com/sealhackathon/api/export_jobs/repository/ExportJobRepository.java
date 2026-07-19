package com.sealhackathon.api.export_jobs.repository;

import com.sealhackathon.api.export_jobs.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExportJobRepository extends JpaRepository<ExportJob, Integer> {

    @Query("SELECT j FROM ExportJob j JOIN FETCH j.hackathon WHERE j.id = :id")
    Optional<ExportJob> findByIdWithHackathon(@Param("id") Integer id);

    @Query("""
            SELECT j FROM ExportJob j
            JOIN FETCH j.hackathon
            WHERE j.hackathon.id = :hackathonId
            ORDER BY j.createdAt DESC
            """)
    List<ExportJob> findByHackathonIdOrderByCreatedAtDesc(@Param("hackathonId") Integer hackathonId);
}
