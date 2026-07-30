package com.sealhackathon.api.kits.repository;

import com.sealhackathon.api.kits.entity.KitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitItemRepository extends JpaRepository<KitItem, Integer> {

    List<KitItem> findByHackathon_IdOrderByIdAsc(Integer hackathonId);

    boolean existsByHackathon_Id(Integer hackathonId);
}
