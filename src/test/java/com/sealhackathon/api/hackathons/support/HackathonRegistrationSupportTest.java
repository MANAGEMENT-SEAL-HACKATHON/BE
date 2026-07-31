package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HackathonRegistrationSupportTest {

    @Test
    void isRegistrationNotYetOpen_whenStartInFuture() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().plusDays(2))
                .registrationEnd(LocalDate.now().plusDays(10))
                .build();
        assertThat(HackathonRegistrationSupport.isRegistrationNotYetOpen(h)).isTrue();
        assertThat(HackathonRegistrationSupport.isRegistrationWindowOpen(h)).isFalse();
    }

    @Test
    void isRegistrationWindowOpen_whenWithinWindow() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(1))
                .registrationEnd(LocalDate.now().plusDays(5))
                .build();
        assertThat(HackathonRegistrationSupport.isRegistrationNotYetOpen(h)).isFalse();
        assertThat(HackathonRegistrationSupport.isRegistrationWindowOpen(h)).isTrue();
    }

    @Test
    void isRegistrationWindowOpen_falseWhenClosed() {
        Hackathon h = Hackathon.builder()
                .status(HackathonStatus.ONGOING)
                .registrationStart(LocalDate.now().minusDays(10))
                .registrationEnd(LocalDate.now().minusDays(1))
                .build();
        assertThat(HackathonRegistrationSupport.isRegistrationWindowOpen(h)).isFalse();
        assertThat(HackathonRegistrationSupport.isRegistrationClosed(h)).isTrue();
    }
}
