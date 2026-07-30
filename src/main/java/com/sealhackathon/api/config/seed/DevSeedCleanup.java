package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xóa hackathon seed deprecated trước khi seed E2E mới (profile dev).
 * Xóa sâu theo {@code hackathon_id} — submissions/scores/teams/rounds… trước khi xóa hackathon.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevSeedCleanup {

    private final HackathonRepository hackathonRepository;
    private final JdbcTemplate jdbcTemplate;

    public void purgeDeprecatedHackathons() {
        int removed = 0;
        for (String slug : DevSeedCatalog.DEPRECATED_SLUGS) {
            try {
                removed += purgeIfPresent(slug);
            } catch (Exception ex) {
                log.error("[DevSeedCleanup] Bỏ qua slug={} — {}", slug, ex.getMessage());
            }
        }
        if (removed > 0) {
            log.info("[DevSeedCleanup] Đã xóa {} hackathon seed cũ", removed);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeIfPresent(String slug) {
        return hackathonRepository.findBySlug(slug)
                .map(this::purgeHackathon)
                .orElse(0);
    }

    private int purgeHackathon(Hackathon hackathon) {
        Integer id = hackathon.getId();
        String slug = hackathon.getSlug();
        try {
            deleteHackathonGraph(id);
            hackathonRepository.delete(hackathon);
            hackathonRepository.flush();
            log.info("[DevSeedCleanup] Removed deprecated hackathon slug={} id={}", slug, id);
            return 1;
        } catch (Exception ex) {
            log.error("[DevSeedCleanup] Không xóa được slug={} id={}: {} — xem SQL trong dev-seed-guide.md",
                    slug, id, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Xóa toàn bộ dữ liệu con theo hackathon_id (thứ tự FK-safe).
     */
    private void deleteHackathonGraph(Integer hackathonId) {
        // Scores & submissions
        jdbcTemplate.update("""
                DELETE s FROM scores s
                INNER JOIN submissions sub ON sub.id = s.submission_id
                WHERE sub.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE sm FROM submission_metadata sm
                INNER JOIN submissions sub ON sub.id = sm.submission_id
                WHERE sub.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("DELETE FROM submissions WHERE hackathon_id = ?", hackathonId);

        // Tiebreak (theo round)
        jdbcTemplate.update("""
                DELETE te FROM tiebreak_evaluations te
                INNER JOIN rounds r ON r.id = te.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);

        // Presentation & prizes
        jdbcTemplate.update("""
                DELETE ps FROM presentation_slots ps
                INNER JOIN teams t ON t.id = ps.team_id
                WHERE t.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("DELETE FROM prizes WHERE hackathon_id = ?", hackathonId);

        // Appeals
        jdbcTemplate.update("""
                DELETE a FROM appeals a
                INNER JOIN teams t ON t.id = a.team_id
                WHERE t.hackathon_id = ?
                """, hackathonId);

        // Mentor / team-round
        jdbcTemplate.update("DELETE FROM mentor_team_assignments WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("""
                DELETE trt FROM team_round_tracks trt
                INNER JOIN teams t ON t.id = trt.team_id
                WHERE t.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("DELETE FROM team_round_participation WHERE hackathon_id = ?", hackathonId);

        // Team members & teams
        jdbcTemplate.update("""
                DELETE tm FROM team_members tm
                INNER JOIN teams t ON t.id = tm.team_id
                WHERE t.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("DELETE FROM teams WHERE hackathon_id = ?", hackathonId);

        // Criteria (self-ref source_criteria_id — xóa scores đã xong)
        jdbcTemplate.update("""
                UPDATE criteria c
                INNER JOIN tracks tr ON tr.id = c.track_id
                INNER JOIN rounds r ON r.id = tr.round_id
                SET c.source_criteria_id = NULL
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                UPDATE criteria c
                INNER JOIN rounds r ON r.id = c.round_id
                SET c.source_criteria_id = NULL
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE c FROM criteria c
                INNER JOIN tracks tr ON tr.id = c.track_id
                INNER JOIN rounds r ON r.id = tr.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE c FROM criteria c
                INNER JOIN rounds r ON r.id = c.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);

        // Judge / mentor assignments
        jdbcTemplate.update("""
                DELETE ja FROM judge_assignments ja
                INNER JOIN tracks tr ON tr.id = ja.track_id
                INNER JOIN rounds r ON r.id = tr.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("""
                DELETE ma FROM mentor_assignments ma
                INNER JOIN tracks tr ON tr.id = ma.track_id
                INNER JOIN rounds r ON r.id = tr.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);

        // Tracks & rounds
        jdbcTemplate.update("""
                DELETE tr FROM tracks tr
                INNER JOIN rounds r ON r.id = tr.round_id
                WHERE r.hackathon_id = ?
                """, hackathonId);
        jdbcTemplate.update("DELETE FROM rounds WHERE hackathon_id = ?", hackathonId);

        // Hackathon-scoped misc
        jdbcTemplate.update("DELETE FROM events WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM invitations WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM export_jobs WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM chapter_rankings WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM individual_rankings WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM hackathon_registrations WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("DELETE FROM hackathon_registration_withdrawals WHERE hackathon_id = ?", hackathonId);
        jdbcTemplate.update("""
                DELETE FROM notifications
                WHERE reference_type = 'hackathons' AND reference_id = ?
                """, hackathonId);
    }
}
