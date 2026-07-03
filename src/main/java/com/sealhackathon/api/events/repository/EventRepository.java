package com.sealhackathon.api.events.repository;

import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.value_object.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    List<Event> findByHackathonIdOrderByStartsAtAsc(Integer hackathonId);

    List<Event> findByHackathonIdAndType(Integer hackathonId, EventType type);

    boolean existsByHackathonIdAndType(Integer hackathonId, EventType type);

    boolean existsByHackathonId(Integer hackathonId);

    /**
     * Lớp 3 helper: lấy event muộn nhất theo {@code endsAt} (fallback {@code startsAt}) cho một type.
     * Dùng cho rule "PRESENTATION trước max KICKOFF.endsAt".
     */
    @Query("""
            SELECT e FROM Event e
            WHERE e.hackathon.id = :hackathonId
              AND e.type = :type
            ORDER BY COALESCE(e.endsAt, e.startsAt) DESC
            """)
    List<Event> findLatestByType(@Param("hackathonId") Integer hackathonId,
                                 @Param("type") EventType type);

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

    /** Public events within lead window that have not yet received EVENT_UPCOMING. */
    @Query("""
            SELECT e FROM Event e
            WHERE e.isPublic = true
              AND e.reminderSentAt IS NULL
              AND e.startsAt > :now
              AND e.startsAt <= :deadline
            """)
    List<Event> findPublicUpcomingWithoutReminder(@Param("now") LocalDateTime now,
                                                  @Param("deadline") LocalDateTime deadline);

    /** OTHER chồng khung giờ milestone (Spring/Fall — tránh lịch phụ đè workshop/kickoff/thi). */
    @Query("""
            SELECT e FROM Event e
            WHERE e.hackathon.id = :hackathonId
              AND e.type = com.sealhackathon.api.events.value_object.EventType.OTHER
              AND e.id <> :excludeId
              AND (
                   (e.endsAt IS NULL AND e.startsAt < :endsAt AND e.startsAt >= :startsAt)
                OR (e.endsAt IS NOT NULL AND e.startsAt < :endsAt AND e.endsAt > :startsAt)
              )
            """)
    List<Event> findOtherOverlapping(@Param("hackathonId") Integer hackathonId,
                                     @Param("startsAt") LocalDateTime startsAt,
                                     @Param("endsAt") LocalDateTime endsAt,
                                     @Param("excludeId") Integer excludeId);

    /** OTHER chồng milestone — kiểm tra đối xứng khi tạo/sửa OTHER. */
    @Query("""
            SELECT e FROM Event e
            WHERE e.hackathon.id = :hackathonId
              AND e.type IN :milestoneTypes
              AND e.id <> :excludeId
              AND (
                   (e.endsAt IS NULL AND e.startsAt < :endsAt AND e.startsAt >= :startsAt)
                OR (e.endsAt IS NOT NULL AND e.startsAt < :endsAt AND e.endsAt > :startsAt)
              )
            """)
    List<Event> findMilestoneOverlapping(@Param("hackathonId") Integer hackathonId,
                                         @Param("milestoneTypes") Collection<EventType> milestoneTypes,
                                         @Param("startsAt") LocalDateTime startsAt,
                                         @Param("endsAt") LocalDateTime endsAt,
                                         @Param("excludeId") Integer excludeId);
}
