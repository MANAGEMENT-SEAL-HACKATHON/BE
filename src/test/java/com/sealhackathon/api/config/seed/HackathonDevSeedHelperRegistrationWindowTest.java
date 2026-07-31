package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class HackathonDevSeedHelperRegistrationWindowTest {

    @Test
    void syncHackathonFields_clearsStaleClosedEarlyFlag_whenRegistrationMovesBackToFuture() throws Exception {
        HackathonRepository hackathonRepository = mock(HackathonRepository.class);
        HackathonDevSeedHelper helper = newHelper(hackathonRepository);

        LocalDate today = LocalDate.now();
        Hackathon hackathon = Hackathon.builder()
                .registrationStart(today.minusDays(3).atTime(0, 0))
                .registrationEnd(today.minusDays(1).atTime(23, 59))
                .registrationClosedEarlyAt(today.minusDays(1).atTime(23, 59))
                .eventStart(today.plusDays(7))
                .eventEnd(today.plusDays(30))
                .year(today.getYear())
                .build();

        HackathonDevSeedHelper.SeedDates dates = new HackathonDevSeedHelper.SeedDates(
                today.minusDays(3),
                today.plusDays(7),
                today.plusDays(8),
                today.plusDays(31),
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(20),
                LocalDateTime.now().plusDays(11),
                LocalDateTime.now().plusDays(21),
                LocalDateTime.now().plusDays(9),
                LocalDateTime.now().plusDays(19));

        boolean changed = invokeSyncHackathonFields(helper, hackathon, dates);

        assertTrue(changed);
        assertNull(hackathon.getRegistrationClosedEarlyAt());
        verify(hackathonRepository).save(hackathon);
    }

    @Test
    void syncHackathonFields_keepsWindowOpenStateUntouched_whenAlreadyOpen() throws Exception {
        HackathonRepository hackathonRepository = mock(HackathonRepository.class);
        HackathonDevSeedHelper helper = newHelper(hackathonRepository);

        LocalDate today = LocalDate.now();
        Hackathon hackathon = Hackathon.builder()
                .registrationStart(today.minusDays(3).atTime(0, 0))
                .registrationEnd(today.plusDays(5).atTime(23, 59))
                .registrationClosedEarlyAt(null)
                .eventStart(today.plusDays(8))
                .eventEnd(today.plusDays(31))
                .year(today.plusDays(8).getYear())
                .build();

        HackathonDevSeedHelper.SeedDates dates = new HackathonDevSeedHelper.SeedDates(
                hackathon.getRegistrationStart().toLocalDate(),
                hackathon.getRegistrationEnd().toLocalDate(),
                hackathon.getEventStart(),
                hackathon.getEventEnd(),
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(20),
                LocalDateTime.now().plusDays(11),
                LocalDateTime.now().plusDays(21),
                LocalDateTime.now().plusDays(9),
                LocalDateTime.now().plusDays(19));

        boolean changed = invokeSyncHackathonFields(helper, hackathon, dates);

        assertFalse(changed);
        verifyNoInteractions(hackathonRepository);
    }

    private static boolean invokeSyncHackathonFields(
            HackathonDevSeedHelper helper,
            Hackathon hackathon,
            HackathonDevSeedHelper.SeedDates dates) throws Exception {
        Method method = HackathonDevSeedHelper.class.getDeclaredMethod(
                "syncHackathonFields", Hackathon.class, HackathonDevSeedHelper.SeedDates.class);
        method.setAccessible(true);
        return (boolean) method.invoke(helper, hackathon, dates);
    }

    private static HackathonDevSeedHelper newHelper(HackathonRepository hackathonRepository) {
        return new HackathonDevSeedHelper(
                null, null, hackathonRepository, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
