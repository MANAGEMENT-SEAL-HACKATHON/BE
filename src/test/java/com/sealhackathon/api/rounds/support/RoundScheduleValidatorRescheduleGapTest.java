package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.config.seed.RoundScheduleSeedUtil;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RoundScheduleValidatorRescheduleGapTest {

    @Mock private HackathonTimelineService hackathonTimelineService;
    @Mock private RoundRepository roundRepository;

    @InjectMocks private RoundScheduleValidator validator;

    @Test
    void requireReschedulePrelimGap_rejectsTooSoon() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder().id(1).registrationEnd(regEnd).build();
        Round prelim = Round.builder().id(3).isFinal(false).hackathon(h).build();
        LocalDateTime tooSoon = regEnd.plusDays(2).atTime(8, 0);

        assertThatThrownBy(() -> validator.requireReschedulePrelimWorkshopKickoffGap(prelim, tooSoon))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Workshop");
    }

    @Test
    void requireReschedulePrelimGap_allowsRegEndPlusThree() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder().id(1).registrationEnd(regEnd).build();
        Round prelim = Round.builder().id(3).isFinal(false).hackathon(h).build();
        LocalDateTime ok = regEnd.plusDays(RoundScheduleSeedUtil.DAYS_REG_END_TO_EVENT_START).atTime(8, 0);

        assertThatCode(() -> validator.requireReschedulePrelimWorkshopKickoffGap(prelim, ok))
                .doesNotThrowAnyException();
    }

    @Test
    void requireReschedulePrelimGap_skipsFinalRound() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder().id(1).registrationEnd(regEnd).build();
        Round finals = Round.builder().id(4).isFinal(true).hackathon(h).build();
        LocalDateTime soon = regEnd.plusDays(1).atTime(8, 0);

        assertThatCode(() -> validator.requireReschedulePrelimWorkshopKickoffGap(finals, soon))
                .doesNotThrowAnyException();
    }
}
