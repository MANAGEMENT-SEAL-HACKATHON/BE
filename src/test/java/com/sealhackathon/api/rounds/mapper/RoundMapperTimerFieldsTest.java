package com.sealhackathon.api.rounds.mapper;

import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RoundMapperTimerFieldsTest {

    private final RoundMapper mapper = new RoundMapper();

    @Test
    void applyUpdate_setsDefaultPresentationAndQaMinutesWhenProvided() {
        Round round = Round.builder()
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();

        UpdateRoundRequest req = UpdateRoundRequest.builder()
                .name("Chung kết")
                .examAt(LocalDateTime.of(2026, 6, 20, 8, 0))
                .submissionDeadline(LocalDateTime.of(2026, 6, 20, 12, 0))
                .defaultPresentationMinutes(12)
                .defaultQaMinutes(8)
                .build();

        mapper.applyUpdate(round, req);

        assertThat(round.getDefaultPresentationMinutes()).isEqualTo(12);
        assertThat(round.getDefaultQaMinutes()).isEqualTo(8);
    }

    @Test
    void applyUpdate_keepsExistingTimerDefaultsWhenOmitted() {
        Round round = Round.builder()
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();

        UpdateRoundRequest req = UpdateRoundRequest.builder()
                .name("Chung kết")
                .examAt(LocalDateTime.of(2026, 6, 20, 8, 0))
                .submissionDeadline(LocalDateTime.of(2026, 6, 20, 12, 0))
                .build();

        mapper.applyUpdate(round, req);

        assertThat(round.getDefaultPresentationMinutes()).isEqualTo(10);
        assertThat(round.getDefaultQaMinutes()).isEqualTo(5);
    }

    @Test
    void toResponse_exposesDefaultPresentationAndQaMinutes() {
        Round round = Round.builder()
                .id(3)
                .name("Sơ loại")
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();

        RoundResponse response = mapper.toResponse(round);

        assertThat(response.getDefaultPresentationMinutes()).isEqualTo(10);
        assertThat(response.getDefaultQaMinutes()).isEqualTo(5);
    }
}
