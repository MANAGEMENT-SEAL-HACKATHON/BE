package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.HackathonProperties;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.dto.response.RegistrationExtensionPreviewResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.CompetitionScheduleAdjustService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundScheduleValidator;
import com.sealhackathon.api.teams.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonRegistrationExtensionServiceImplTest {

    @Mock private HackathonRepository hackathonRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private EventRepository eventRepository;
    @Mock private HackathonProperties hackathonProperties;
    @Mock private CompetitionScheduleAdjustService competitionScheduleAdjustService;
    @Mock private StakeholderBroadcastService stakeholderBroadcastService;
    @Mock private AuditService auditService;

    @InjectMocks private HackathonRegistrationExtensionServiceImpl service;

    private LocalDateTime currentEnd;
    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        currentEnd = LocalDate.now().plusDays(5).atTime(23, 59);
        hackathon = Hackathon.builder()
                .id(1)
                .name("SEAL")
                .status(HackathonStatus.ONGOING)
                .registrationEnd(currentEnd)
                .eventStart(currentEnd.toLocalDate().plusDays(RoundScheduleValidator.MIN_DAYS_FROM_REG_END_TO_PRELIM_EXAM))
                .registrationExtensionCount(0)
                .build();
        org.mockito.Mockito.lenient().when(hackathonProperties.getMaxRegistrationExtensions()).thenReturn(2);
    }

    @Test
    void preview_gapStatuses_okTightViolation() {
        LocalDateTime newEnd = currentEnd.plusDays(4);
        LocalDate newEndDay = newEnd.toLocalDate();
        // WS on newEnd+1 → OK; KO on newEnd (not after) → VIOLATION
        Event ws = Event.builder().id(10).startsAt(newEndDay.plusDays(1).atTime(20, 0)).build();
        Event ko = Event.builder().id(11).startsAt(newEndDay.atTime(14, 0)).build();
        // prelim at newEnd+3 → TIGHT; eventStart at newEnd+2 → VIOLATION (< 3)
        Round prelim = Round.builder()
                .id(3).isFinal(false)
                .examAt(newEndDay.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START).atTime(8, 0))
                .build();
        hackathon.setEventStart(newEndDay.plusDays(2));

        when(hackathonRepository.findById(1)).thenReturn(Optional.of(hackathon));
        when(teamRepository.findByHackathon_Id(1)).thenReturn(List.of());
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP)).thenReturn(List.of(ws));
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF)).thenReturn(List.of(ko));
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(List.of(prelim));

        RegistrationExtensionPreviewResponse preview = service.preview(1, newEnd);

        assertThat(preview.getExtensionCount()).isZero();
        assertThat(preview.getMaxExtensions()).isEqualTo(2);
        assertThat(statusOf(preview, "WORKSHOP")).isEqualTo("OK");
        assertThat(statusOf(preview, "KICKOFF")).isEqualTo("VIOLATION");
        assertThat(statusOf(preview, "PRELIM")).isEqualTo("TIGHT");
        assertThat(statusOf(preview, "EVENT_START")).isEqualTo("VIOLATION");
        assertThat(preview.isCanExtend()).isFalse();
        assertThat(preview.getBlockReason()).contains("xung đột");
        assertThat(preview.getSuggestedAdjustments()).isNotEmpty();
    }

    @Test
    void preview_limitReached_blocks() {
        hackathon.setRegistrationExtensionCount(2);
        LocalDateTime newEnd = currentEnd.plusDays(3);
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(hackathon));
        when(teamRepository.findByHackathon_Id(1)).thenReturn(List.of());

        RegistrationExtensionPreviewResponse preview = service.preview(1, newEnd);

        assertThat(preview.isCanExtend()).isFalse();
        assertThat(preview.getBlockReason()).contains("2/2");
        assertThat(preview.getMilestones()).isEmpty();
    }

    @Test
    void extend_limitReached_throws() {
        hackathon.setRegistrationExtensionCount(2);
        when(hackathonRepository.findByIdForUpdate(1)).thenReturn(Optional.of(hackathon));

        assertThatThrownBy(() -> service.extend(1,
                com.sealhackathon.api.hackathons.dto.request.RegistrationExtensionRequest.builder()
                        .newRegistrationEnd(currentEnd.plusDays(3))
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.REGISTRATION_EXTENSION_LIMIT_REACHED);
    }

    @Test
    void gapStatusHelpers_matchConstants() {
        LocalDate reg = LocalDate.of(2026, 8, 1);
        assertThat(HackathonRegistrationExtensionServiceImpl.gapStatusForPrelim(
                reg.plusDays(2), reg)).isEqualTo("VIOLATION");
        assertThat(HackathonRegistrationExtensionServiceImpl.gapStatusForPrelim(
                reg.plusDays(3), reg)).isEqualTo("TIGHT");
        assertThat(HackathonRegistrationExtensionServiceImpl.gapStatusForPrelim(
                reg.plusDays(5), reg)).isEqualTo("OK");
        assertThat(HackathonRegistrationExtensionServiceImpl.gapStatusForWorkshopKickoff(
                reg, reg, reg.plusDays(5))).isEqualTo("VIOLATION");
        assertThat(HackathonRegistrationExtensionServiceImpl.gapStatusForWorkshopKickoff(
                reg.plusDays(1), reg, reg.plusDays(5))).isEqualTo("OK");
    }

    private static String statusOf(RegistrationExtensionPreviewResponse preview, String key) {
        return preview.getMilestones().stream()
                .filter(m -> key.equals(m.getKey()))
                .map(RegistrationExtensionPreviewResponse.MilestoneItem::getStatus)
                .findFirst()
                .orElse(null);
    }
}
