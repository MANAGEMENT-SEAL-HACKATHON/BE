package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.RegistrationPhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HackathonRegistrationSupportTest {

    @Test
    void isRegistrationNotYetOpen_whenStartInFuture() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().plusDays(2).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(10).atTime(23, 59))
                .build();
        assertThat(HackathonRegistrationSupport.isRegistrationNotYetOpen(h)).isTrue();
        assertThat(HackathonRegistrationSupport.isRegistrationWindowOpen(h)).isFalse();
        assertThat(HackathonRegistrationSupport.resolveRegistrationPhase(h))
                .isEqualTo(RegistrationPhase.NOT_YET_OPEN);
    }

    @Test
    void isRegistrationWindowOpen_whenWithinWindow() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(1).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(5).atTime(23, 59))
                .build();
        assertThat(HackathonRegistrationSupport.isRegistrationNotYetOpen(h)).isFalse();
        assertThat(HackathonRegistrationSupport.isRegistrationWindowOpen(h)).isTrue();
        assertThat(HackathonRegistrationSupport.resolveRegistrationPhase(h))
                .isEqualTo(RegistrationPhase.OPEN);
    }

    @Test
    void isRegistrationWindowOpen_falseWhenClosed() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(10).atTime(0, 0))
                .registrationEnd(LocalDate.now().minusDays(1).atTime(23, 59))
                .build();
        assertThat(HackathonRegistrationSupport.isRegistrationWindowOpen(h)).isFalse();
        assertThat(HackathonRegistrationSupport.isRegistrationClosed(h)).isTrue();
        assertThat(HackathonRegistrationSupport.resolveRegistrationPhase(h))
                .isEqualTo(RegistrationPhase.CLOSED);
    }

    @Test
    void resolveRegistrationPhase_closedEarly() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(5).atTime(0, 0))
                .registrationEnd(LocalDate.now().plusDays(5).atTime(23, 59))
                .registrationClosedEarlyAt(LocalDateTime.now().minusHours(1))
                .build();
        assertThat(HackathonRegistrationSupport.resolveRegistrationPhase(h))
                .isEqualTo(RegistrationPhase.CLOSED_EARLY);
    }
}
