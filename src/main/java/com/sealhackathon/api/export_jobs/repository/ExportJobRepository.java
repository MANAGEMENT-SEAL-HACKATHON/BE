package com.sealhackathon.api.export_jobs.repository;

import com.sealhackathon.api.export_jobs.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJob, Integer> {
}
