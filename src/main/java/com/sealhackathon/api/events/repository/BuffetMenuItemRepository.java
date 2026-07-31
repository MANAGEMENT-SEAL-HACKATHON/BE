package com.sealhackathon.api.events.repository;

import com.sealhackathon.api.events.entity.BuffetMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuffetMenuItemRepository extends JpaRepository<BuffetMenuItem, Integer> {

    List<BuffetMenuItem> findByEvent_IdOrderByDisplayOrderAscIdAsc(Integer eventId);

    void deleteByEvent_Id(Integer eventId);
}
