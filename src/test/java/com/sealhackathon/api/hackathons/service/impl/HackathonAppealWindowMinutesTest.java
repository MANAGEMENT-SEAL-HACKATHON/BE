package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.mapper.HackathonMapper;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.support.HackathonBannerStorageService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HackathonAppealWindowMinutesTest {

    @Mock private HackathonRepository hackathonRepository;
    @Mock private HackathonMapper hackathonMapper;
    @Mock private AuditService auditService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private TrackRepository trackRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private EventRepository eventRepository;
    @Mock private HackathonArchiveGuard archiveGuard;
    @Mock private HackathonBannerStorageService bannerStorageService;
    @Mock private HackathonCloneSupport hackathonCloneSupport;

    @InjectMocks private HackathonServiceImpl service;

    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        hackathon = Hackathon.builder()
                .id(1)
                .status(HackathonStatus.ONGOING)
                .appealWindowMinutes(30)
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(hackathon));
        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(hackathonMapper.toResponse(any())).thenAnswer(inv -> {
            Hackathon h = inv.getArgument(0);
            return HackathonResponse.builder()
                    .id(h.getId())
                    .appealWindowMinutes(h.getAppealWindowMinutes())
                    .status(h.getStatus())
                    .build();
        });
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of(
                Round.builder().id(10).isPublished(false).build()));
    }

    @Test
    void updateAppealWindowMinutes_ongoingOkBeforePublish() {
        HackathonResponse resp = service.updateAppealWindowMinutes(1, 20);
        assertThat(resp.getAppealWindowMinutes()).isEqualTo(20);
        assertThat(hackathon.getAppealWindowMinutes()).isEqualTo(20);
    }

    @Test
    void updateAppealWindowMinutes_blockedAfterPublish() {
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of(
                Round.builder().id(10).isPublished(true).build()));

        assertThatThrownBy(() -> service.updateAppealWindowMinutes(1, 20))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_WINDOW_LOCKED_AFTER_PUBLISH);
    }

    @Test
    void updateAppealWindowMinutes_belowMinimum_rejected() {
        assertThatThrownBy(() -> service.updateAppealWindowMinutes(1, 5))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_WINDOW_BELOW_MINIMUM);
    }
}
