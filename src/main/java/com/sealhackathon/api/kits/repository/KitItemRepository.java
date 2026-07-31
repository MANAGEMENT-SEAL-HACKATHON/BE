package com.sealhackathon.api.kits.repository;

import com.sealhackathon.api.kits.entity.KitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitItemRepository extends JpaRepository<KitItem, Integer> {

    List<KitItem> findByHackathon_IdOrderByIdAsc(Integer hackathonId);

    boolean existsByHackathon_Id(Integer hackathonId);

    boolean existsByHackathon_IdAndNameIgnoreCase(Integer hackathonId, String name);

    @Query("""
            SELECT DISTINCT i.hackathon.id FROM KitItem i
            WHERE i.hackathon.id <> :excludeHackathonId
            """)
    List<Integer> findDistinctHackathonIdsWithKitsExcluding(@Param("excludeHackathonId") Integer excludeHackathonId);

    long countByHackathon_Id(Integer hackathonId);
}
