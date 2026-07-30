package com.sealhackathon.api.kits.repository;

import com.sealhackathon.api.kits.entity.KitAllocation;
import com.sealhackathon.api.kits.value_object.KitAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KitAllocationRepository extends JpaRepository<KitAllocation, Integer> {

    List<KitAllocation> findByHackathon_Id(Integer hackathonId);

    List<KitAllocation> findByHackathon_IdAndUser_IdIn(Integer hackathonId, Collection<Integer> userIds);

    Optional<KitAllocation> findByHackathon_IdAndUser_IdAndKitItem_Id(
            Integer hackathonId, Integer userId, Integer kitItemId);

    long countByHackathon_IdAndKitItem_IdAndStatus(
            Integer hackathonId, Integer kitItemId, KitAllocationStatus status);

    @Query("""
            SELECT COUNT(a) FROM KitAllocation a
            WHERE a.hackathon.id = :hackathonId
              AND a.kitItem.id = :kitItemId
              AND a.status = :status
              AND ((:sizeKey = '' AND a.size IS NULL) OR a.size = :size)
            """)
    long countIssuedByItemAndSize(
            @Param("hackathonId") Integer hackathonId,
            @Param("kitItemId") Integer kitItemId,
            @Param("status") KitAllocationStatus status,
            @Param("size") String size,
            @Param("sizeKey") String sizeKey);

    void deleteByKitItem_Id(Integer kitItemId);
}
