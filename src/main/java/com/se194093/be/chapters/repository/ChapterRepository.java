package com.se194093.be.chapters.repository;

import com.se194093.be.chapters.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {

    Optional<Chapter> findByCode(String code);

    boolean existsByCode(String code);
}
