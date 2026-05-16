package com.se194093.be.events.repository;

import com.se194093.be.events.entity.Event;
import com.se194093.be.events.value_object.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    List<Event> findByHackathonIdOrderByStartsAtAsc(Integer hackathonId);

    List<Event> findByHackathonIdAndType(Integer hackathonId, EventType type);

    boolean existsByHackathonIdAndType(Integer hackathonId, EventType type);

    /**
     * Lớp 2 overlap check — tìm các event cùng type trong khoảng giao với (start, end).
     * Bỏ qua chính event đang sửa (excludeId optional, dùng 0 hoặc -1 cho POST).
     */
    @Query("""
            SELECT e FROM Event e
            WHERE e.hackathon.id = :hackathonId
              AND e.type = :type
              AND e.id <> :excludeId
              AND (
                   (e.endsAt IS NULL AND e.startsAt < :endsAt AND e.startsAt >= :startsAt)
                OR (e.endsAt IS NOT NULL AND e.startsAt < :endsAt AND e.endsAt > :startsAt)
              )
            """)
    List<Event> findOverlapping(@Param("hackathonId") Integer hackathonId,
                                @Param("type") EventType type,
                                @Param("startsAt") LocalDateTime startsAt,
                                @Param("endsAt") LocalDateTime endsAt,
                                @Param("excludeId") Integer excludeId);

    @Query("""
            SELECT e FROM Event e
            WHERE e.hackathon.id = :hackathonId
              AND e.startsAt BETWEEN :from AND :to
            ORDER BY e.startsAt ASC
            """)
    List<Event> findInRange(@Param("hackathonId") Integer hackathonId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);
}
