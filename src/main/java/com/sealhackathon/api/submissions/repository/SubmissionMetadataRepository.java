package com.sealhackathon.api.submissions.repository;

import com.sealhackathon.api.submissions.entity.SubmissionMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionMetadataRepository extends JpaRepository<SubmissionMetadata, Integer> {
}
