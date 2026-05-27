package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HackathonArchiveGuardTest {

    private final HackathonArchiveGuard guard = new HackathonArchiveGuard();

    @Test
    void assertNotArchived_ongoing_doesNotThrow() {
        Hackathon h = Hackathon.builder().id(1).status(HackathonStatus.ONGOING).build();
        assertThatCode(() -> guard.assertNotArchived(h)).doesNotThrowAnyException();
    }

    @Test
    void assertNotArchived_finished_throwsArchived() {
        Hackathon h = Hackathon.builder().id(99).status(HackathonStatus.FINISHED).build();
        assertThatThrownBy(() -> guard.assertNotArchived(h))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.HACKATHON_ARCHIVED.equals(((ConflictException) ex).getCode()));
    }

    @Test
    void assertNotArchivedForCriteria_onFinishedRound_throws() {
        Hackathon h = Hackathon.builder().id(2).status(HackathonStatus.FINISHED).build();
        Round round = Round.builder().id(10).hackathon(h).build();
        Criteria c = Criteria.builder().id(5).round(round).build();
        assertThatThrownBy(() -> guard.assertNotArchivedForCriteria(c))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.HACKATHON_ARCHIVED.equals(((ConflictException) ex).getCode()));
    }
}
