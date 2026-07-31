package com.sealhackathon.api.kits.repository;

import com.sealhackathon.api.kits.entity.KitBundleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KitBundleItemRepository extends JpaRepository<KitBundleItem, Integer> {

    boolean existsByKitItem_Id(Integer kitItemId);

    void deleteByBundle_Id(Integer bundleId);
}
