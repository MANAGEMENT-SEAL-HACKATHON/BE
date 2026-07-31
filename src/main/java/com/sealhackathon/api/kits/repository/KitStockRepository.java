package com.sealhackathon.api.kits.repository;

import com.sealhackathon.api.kits.entity.KitStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KitStockRepository extends JpaRepository<KitStock, Integer> {

    List<KitStock> findByKitItem_IdOrderBySizeAsc(Integer kitItemId);

    List<KitStock> findByKitItem_IdIn(Collection<Integer> kitItemIds);

    Optional<KitStock> findByKitItem_IdAndFitKeyAndSizeKey(Integer kitItemId, String fitKey, String sizeKey);

    void deleteByKitItem_Id(Integer kitItemId);
}
