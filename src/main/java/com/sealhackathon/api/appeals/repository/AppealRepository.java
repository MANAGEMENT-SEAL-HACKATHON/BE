package com.sealhackathon.api.appeals.repository;

import com.sealhackathon.api.appeals.entity.Appeal;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Integer> {

    boolean existsByTeam_IdAndRound_Id(Integer teamId, Integer roundId);

    List<Appeal> findByRound_IdOrderByCreatedAtDesc(Integer roundId);

    List<Appeal> findByRound_IdAndStatusOrderByCreatedAtDesc(Integer roundId, AppealStatus status);

    List<Appeal> findByTeam_IdOrderByCreatedAtDesc(Integer teamId);

    List<Appeal> findByTeam_IdInOrderByCreatedAtDesc(Collection<Integer> teamIds);

    long countByRound_IdAndStatus(Integer roundId, AppealStatus status);

    long countByRound_IdAndStatusIn(Integer roundId, Collection<AppealStatus> statuses);

    boolean existsByRound_IdAndStatusIn(Integer roundId, Collection<AppealStatus> statuses);

    @Query("""
            SELECT a FROM Appeal a
             WHERE a.round.id = :roundId
               AND a.status IN :statuses
            """)
    List<Appeal> findByRoundIdAndStatusIn(@Param("roundId") Integer roundId,
                                          @Param("statuses") Collection<AppealStatus> statuses);

    @Query("""
            SELECT DISTINCT a.round.id FROM Appeal a
             JOIN a.round r
             WHERE r.appealWindowEndsAt IS NOT NULL
               AND r.appealWindowEndsAt <= :now
               AND a.status IN :statuses
            """)
    List<Integer> findRoundIdsWithExpiredOpenAppeals(
            @Param("now") java.time.LocalDateTime now,
            @Param("statuses") Collection<AppealStatus> statuses);
}
