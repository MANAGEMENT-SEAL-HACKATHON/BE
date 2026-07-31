package com.sealhackathon.api.events.support;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuffetEditGuardTest {

    @Mock private RoundRepository roundRepository;
    @InjectMocks private BuffetEditGuard guard;

    @Test
    void allowsEditWhenPrelimNotPublished() {
        when(roundRepository.findPreliminaryLikeByHackathonId(1))
                .thenReturn(List.of(Round.builder().id(10).isPublished(false).build()));

        assertDoesNotThrow(() -> guard.assertEditable(1));
    }

    @Test
    void blocksEditWhenPrelimPublished() {
        when(roundRepository.findPreliminaryLikeByHackathonId(1))
                .thenReturn(List.of(Round.builder().id(10).isPublished(true).build()));

        assertEquals(ErrorCode.BUFFET_LOCKED_AFTER_PUBLISH,
                assertThrows(BusinessRuleException.class, () -> guard.assertEditable(1)).getCode());
    }

    @Test
    void allowsEditWhenNoPrelim() {
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(Collections.emptyList());
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> guard.assertEditable(1));
    }
}
