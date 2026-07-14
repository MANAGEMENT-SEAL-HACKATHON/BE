package com.sealhackathon.api.calibration_sessions.repository;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.config.CriteriaCloneSourceUnlinkMigration;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository IT (H2) — replaces @DataJpaTest (not on this Spring Boot classpath).
 * Covers existsByRound…Track… and existsByRound…TrackIsNull….
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:calib-repo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012",
        "security.jwt.enabled=true",
        "app.storage.type=local",
        "app.storage.local-dir=target/test-uploads/calib-repo"
})
class CalibrationSessionRepositoryTest {

    @Autowired private CalibrationSessionRepository repository;
    @Autowired private HackathonRepository hackathonRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private TrackRepository trackRepository;

    @MockitoBean
    private CriteriaCloneSourceUnlinkMigration criteriaCloneSourceUnlinkMigration;

    private Round round;
    private Track trackA;

    @BeforeEach
    void setUp() {
        Hackathon hackathon = hackathonRepository.save(Hackathon.builder()
                .name("Calib Repo Test")
                .slug("calib-repo-test-" + System.nanoTime())
                .season(Season.Spring)
                .year(2026)
                .status(HackathonStatus.ONGOING)
                .wildcardEnabled(false)
                .individualRankingEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        round = roundRepository.save(Round.builder()
                .hackathon(hackathon)
                .name("Sơ loại")
                .isFinal(false)
                .roundType(RoundType.PRELIMINARY)
                .submissionDeadline(LocalDateTime.now().plusDays(7))
                .lateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING)
                .scoringLocked(false)
                .build());

        trackA = trackRepository.save(Track.builder()
                .round(round)
                .name("Track A")
                .status(TrackStatus.OPEN)
                .minTeamSize(3)
                .maxTeamSize(5)
                .sequenceOrder(1)
                .build());
    }

    @Test
    void existsByRoundAndTrackAndStatus_andNullTrack_independent() {
        repository.save(CalibrationSession.builder()
                .round(round)
                .track(trackA)
                .status(CalibrationStatus.OPEN)
                .targetScore(8f)
                .startedAt(LocalDateTime.now())
                .build());
        CalibrationSession nullTrack = repository.save(CalibrationSession.builder()
                .round(round)
                .track(null)
                .status(CalibrationStatus.OPEN)
                .targetScore(8f)
                .startedAt(LocalDateTime.now())
                .build());

        assertThat(repository.existsByRound_IdAndTrack_IdAndStatus(
                round.getId(), trackA.getId(), CalibrationStatus.OPEN)).isTrue();
        assertThat(repository.existsByRound_IdAndTrack_IdAndStatus(
                round.getId(), 99999, CalibrationStatus.OPEN)).isFalse();
        assertThat(repository.existsByRound_IdAndTrackIsNullAndStatus(
                round.getId(), CalibrationStatus.OPEN)).isTrue();

        nullTrack.setStatus(CalibrationStatus.CLOSED);
        repository.save(nullTrack);

        assertThat(repository.existsByRound_IdAndTrackIsNullAndStatus(
                round.getId(), CalibrationStatus.OPEN)).isFalse();
        assertThat(repository.existsByRound_IdAndTrack_IdAndStatus(
                round.getId(), trackA.getId(), CalibrationStatus.OPEN)).isTrue();
    }
}
