package com.sealhackathon.api.rounds.guard;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.exception.ScoringLockedException;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundAccessGuardForUpdateTest {

    @Mock
    private RoundRepository roundRepository;

    @InjectMocks
    private RoundAccessGuard guard;

    @Test
    void requireActiveRoundForUpdate_usesPessimisticFind() {
        Round round = Round.builder().id(7).isActive(true).build();
        when(roundRepository.findByIdForUpdate(7)).thenReturn(Optional.of(round));

        assertSame(round, guard.requireActiveRoundForUpdate(7));
        verify(roundRepository, times(1)).findByIdForUpdate(7);
    }

    @Test
    void requireActiveRoundForUpdate_missing_throwsNotFound() {
        when(roundRepository.findByIdForUpdate(8)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> guard.requireActiveRoundForUpdate(8));
    }

    @Test
    void requireActiveRoundForUpdate_inactive_throws() {
        when(roundRepository.findByIdForUpdate(9)).thenReturn(Optional.of(
                Round.builder().id(9).isActive(false).build()));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> guard.requireActiveRoundForUpdate(9));
        assertEquals(ErrorCode.ROUND_NOT_ACTIVE, ex.getCode());
        assertEquals("Round chưa được kích hoạt", ex.getMessage());
    }

    @Test
    void requireActiveRound_inactiveAfterActivation_reportsEnded() {
        Round round = Round.builder()
                .id(10)
                .isActive(false)
                .activatedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(roundRepository.findById(10)).thenReturn(Optional.of(round));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> guard.requireActiveRound(10));

        assertEquals(ErrorCode.ROUND_NOT_ACTIVE, ex.getCode());
        assertEquals("Vòng thi đã kết thúc", ex.getMessage());
    }

    @Test
    void requireActiveRoundForUpdate_inactiveAfterActivation_reportsEnded() {
        Round round = Round.builder()
                .id(11)
                .isActive(false)
                .activatedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(roundRepository.findByIdForUpdate(11)).thenReturn(Optional.of(round));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> guard.requireActiveRoundForUpdate(11));

        assertEquals(ErrorCode.ROUND_NOT_ACTIVE, ex.getCode());
        assertEquals("Vòng thi đã kết thúc", ex.getMessage());
    }

    @Test
    void requireUnlockedRound_lockedAfterActivation_reportsEnded() {
        Round round = Round.builder()
                .id(12)
                .isActive(true)
                .scoringLocked(true)
                .activatedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(roundRepository.findById(12)).thenReturn(Optional.of(round));

        ScoringLockedException ex = assertThrows(ScoringLockedException.class,
                () -> guard.requireUnlockedRound(12));

        assertEquals("Vòng thi đã kết thúc", ex.getMessage());
    }

    /**
     * Documents concurrent contract: second waiter must re-read after lock release.
     * Latch coordinates two threads hitting forUpdate (mocked sequential returns).
     */
    @Test
    void concurrentClosePath_secondSeesAlreadyClosedAfterFirst() throws Exception {
        Round open = Round.builder().id(42).isActive(true).scoringLocked(false).build();
        Round closed = Round.builder()
                .id(42)
                .isActive(true)
                .scoringLocked(false)
                .submissionClosedEarlyAt(java.time.LocalDateTime.now())
                .build();

        AtomicInteger calls = new AtomicInteger();
        when(roundRepository.findByIdForUpdate(42)).thenAnswer(inv -> {
            int n = calls.incrementAndGet();
            return Optional.of(n == 1 ? open : closed);
        });

        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger alreadyClosed = new AtomicInteger();

        Runnable worker = () -> {
            try {
                bothReady.countDown();
                start.await();
                Round r = guard.requireActiveRoundForUpdate(42);
                if (r.getSubmissionClosedEarlyAt() != null) {
                    alreadyClosed.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread t1 = new Thread(worker);
        Thread t2 = new Thread(worker);
        t1.start();
        t2.start();
        bothReady.await();
        start.countDown();
        t1.join();
        t2.join();

        verify(roundRepository, times(2)).findByIdForUpdate(42);
        assertEquals(1, alreadyClosed.get());
    }
}
