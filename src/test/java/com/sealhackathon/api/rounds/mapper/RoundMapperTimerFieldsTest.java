package com.sealhackathon.api.rounds.mapper;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TC-BE-01..04 — timer fields on create/update + Bean Validation boundaries.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundMapperTimerFieldsTest {

    @Mock
    private RoundPresentationReadiness roundPresentationReadiness;

    private RoundMapper mapper;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @BeforeEach
    void setUp() {
        mapper = new RoundMapper(roundPresentationReadiness);
        when(roundPresentationReadiness.evaluate(any())).thenReturn(
                RoundPresentationReadiness.Flags.builder()
                        .presentationShuffled(false)
                        .presentationsComplete(false)
                        .build());
    }

    private static CreateRoundRequest.CreateRoundRequestBuilder baseCreate(boolean isFinal) {
        LocalDateTime exam = LocalDateTime.of(2026, 6, 20, 8, 0);
        return CreateRoundRequest.builder()
                .name(isFinal ? "Chung kết" : "Sơ loại")
                .examAt(exam)
                .isFinal(isFinal)
                .submissionDeadline(exam.plusHours(4));
    }

    /** TC-BE-01 — Create Final persists 15/8 */
    @Test
    void tcBe01_toEntity_final_persistsCustomTimer() {
        CreateRoundRequest req = baseCreate(true)
                .defaultPresentationMinutes(15)
                .defaultQaMinutes(8)
                .build();

        Round entity = mapper.toEntity(req, Hackathon.builder().id(1).build());

        assertThat(entity.getDefaultPresentationMinutes()).isEqualTo(15);
        assertThat(entity.getDefaultQaMinutes()).isEqualTo(8);
    }

    /** TC-BE-02 — timer 0 / 61 fail Bean Validation */
    @Test
    void tcBe02_createRequest_rejectsTimerOutOfRange() {
        CreateRoundRequest zero = baseCreate(true).defaultPresentationMinutes(0).build();
        CreateRoundRequest over = baseCreate(true).defaultQaMinutes(61).build();

        Set<ConstraintViolation<CreateRoundRequest>> z = validator.validate(zero);
        Set<ConstraintViolation<CreateRoundRequest>> o = validator.validate(over);

        assertThat(z).isNotEmpty();
        assertThat(o).isNotEmpty();
        assertThat(z.stream().anyMatch(v -> v.getPropertyPath().toString().contains("Presentation")
                || v.getPropertyPath().toString().contains("presentation"))).isTrue();
    }

    /** TC-BE-03 — Prelimpaysload timer ignored → entity keeps defaults 10/5 (not client 15) */
    @Test
    void tcBe03_toEntity_prelim_ignoresClientTimerPayload() {
        CreateRoundRequest req = baseCreate(false)
                .defaultPresentationMinutes(15)
                .defaultQaMinutes(8)
                .topNAdvance(2)
                .minTeamsFinal(6)
                .build();

        Round entity = mapper.toEntity(req, Hackathon.builder().id(1).build());

        assertThat(entity.getDefaultPresentationMinutes()).isEqualTo(10);
        assertThat(entity.getDefaultQaMinutes()).isEqualTo(5);
    }

    /** TC-BE-04 — Update Final changes timer 15/8 → 20/10 */
    @Test
    void tcBe04_applyUpdate_final_updatesTimer() {
        Round round = Round.builder()
                .isFinal(true)
                .defaultPresentationMinutes(15)
                .defaultQaMinutes(8)
                .build();

        UpdateRoundRequest req = UpdateRoundRequest.builder()
                .name("Chung kết")
                .examAt(LocalDateTime.of(2026, 6, 20, 8, 0))
                .submissionDeadline(LocalDateTime.of(2026, 6, 20, 12, 0))
                .defaultPresentationMinutes(20)
                .defaultQaMinutes(10)
                .build();

        mapper.applyUpdate(round, req);

        assertThat(round.getDefaultPresentationMinutes()).isEqualTo(20);
        assertThat(round.getDefaultQaMinutes()).isEqualTo(10);
        assertThat(round.getName()).isEqualTo("Chung kết");
    }

    @Test
    void applyUpdate_prelim_ignoresTimerPayload() {
        Round round = Round.builder()
                .isFinal(false)
                .defaultPresentationMinutes(10)
                .defaultQaMinutes(5)
                .build();

        UpdateRoundRequest req = UpdateRoundRequest.builder()
                .name("Sơ loại")
                .examAt(LocalDateTime.of(2026, 6, 10, 8, 0))
                .submissionDeadline(LocalDateTime.of(2026, 6, 10, 12, 0))
                .defaultPresentationMinutes(20)
                .defaultQaMinutes(10)
                .build();

        mapper.applyUpdate(round, req);

        assertThat(round.getDefaultPresentationMinutes()).isEqualTo(10);
        assertThat(round.getDefaultQaMinutes()).isEqualTo(5);
    }

    @Test
    void applyUpdate_keepsExistingTimerDefaultsWhenOmittedOnFinal() {
        Round round = Round.builder()
                .isFinal(true)
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

    @Test
    void updateRoundRequest_hasTimerFieldsWithMax60() {
        UpdateRoundRequest over = UpdateRoundRequest.builder()
                .name("CK")
                .examAt(LocalDateTime.of(2026, 6, 20, 8, 0))
                .submissionDeadline(LocalDateTime.of(2026, 6, 20, 12, 0))
                .defaultPresentationMinutes(61)
                .build();
        assertThat(validator.validate(over)).isNotEmpty();
    }
}
