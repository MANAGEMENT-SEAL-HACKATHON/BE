package com.sealhackathon.api.submissions.repository;

import com.sealhackathon.api.submissions.entity.SubmissionMetadata;
import com.sealhackathon.api.submissions.value_object.MetadataFetchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionMetadataRepository extends JpaRepository<SubmissionMetadata, Integer> {

    List<SubmissionMetadata> findTop50ByMetadataFetchStatusOrderBySubmissionIdAsc(
            MetadataFetchStatus metadataFetchStatus);
}
