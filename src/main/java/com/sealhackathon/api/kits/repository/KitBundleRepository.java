package com.sealhackathon.api.kits.repository;

import com.sealhackathon.api.kits.entity.KitBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitBundleRepository extends JpaRepository<KitBundle, Integer> {

    List<KitBundle> findByHackathon_IdOrderByIdAsc(Integer hackathonId);

    List<KitBundle> findByHackathon_IdAndIsDefaultTrue(Integer hackathonId);
}
