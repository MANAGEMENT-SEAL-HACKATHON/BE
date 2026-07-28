package com.sealhackathon.api.me.mentor.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.me.support.MentorAccessGuard;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MentorPortalServiceImplTest {

    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    @Mock private MentorAccessGuard mentorAccessGuard;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private RoundRankingQueryService roundRankingQueryService;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private PresentationSlotRepository presentationSlotRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks
    private MentorPortalServiceImpl service;

    @Test
    void listTeamScores_whenRoundNotLocked_throwsRoundNotScoringLocked() {
        doNothing().when(mentorAccessGuard).assertAssignedToTeam(5);
        when(roundRepository.findById(10)).thenReturn(Optional.of(Round.builder()
                .id(10)
                .name("Prelim")
                .scoringLocked(false)
                .build()));

        assertThatThrownBy(() -> service.listTeamScores(5, 10))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.ROUND_NOT_SCORING_LOCKED);
    }

    @Test
    void getFinalRoundSchedule_whenNotFinal_throwsInvalidFinalRound() {
        when(roundRepository.findById(10)).thenReturn(Optional.of(Round.builder()
                .id(10)
                .name("Prelim")
                .isFinal(false)
                .build()));

        assertThatThrownBy(() -> service.getFinalRoundSchedule(10))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_FINAL_ROUND);
    }

    @Test
    void getHackathonRankings_beforePendingConfirm_throwsResultNotAvailable() {
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(Hackathon.builder()
                .id(1)
                .status(HackathonStatus.ONGOING)
                .build()));

        assertThatThrownBy(() -> service.getHackathonRankings(1))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo("RESULT_NOT_AVAILABLE");
    }
}
