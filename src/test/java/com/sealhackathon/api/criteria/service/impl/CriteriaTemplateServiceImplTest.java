package com.sealhackathon.api.criteria.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.entity.CriteriaTemplate;
import com.sealhackathon.api.criteria.entity.CriteriaTemplateItem;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.repository.CriteriaTemplateRepository;
import com.sealhackathon.api.criteria.service.CriteriaTemplateService;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriteriaTemplateServiceImplTest {
    @Mock CriteriaTemplateRepository templateRepository;
    @Mock CriteriaRepository criteriaRepository;
    @Mock TrackRepository trackRepository;
    @Mock RoundRepository roundRepository;
    @Mock ScoreRepository scoreRepository;
    @InjectMocks CriteriaTemplateServiceImpl service;

    @Test
    void applyToEmptyTrackCopiesTemplateItems() {
        CriteriaTemplate template = template();
        Track track = Track.builder().id(9).build();
        when(templateRepository.findById(1)).thenReturn(Optional.of(template));
        when(trackRepository.findById(9)).thenReturn(Optional.of(track));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(9)).thenReturn(List.of());
        when(criteriaRepository.save(any(Criteria.class))).thenAnswer(inv -> {
            Criteria criterion = inv.getArgument(0);
            criterion.setId(100);
            return criterion;
        });

        var response = service.applyToTrack(1, 9, false);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.createdIds()).containsExactly(100);
    }

    @Test
    void applyToNonEmptyTrackWithoutReplaceRejects() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(template()));
        when(trackRepository.findById(9)).thenReturn(Optional.of(Track.builder().id(9).build()));
        when(criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(9))
                .thenReturn(List.of(Criteria.builder().id(4).build()));

        assertThatThrownBy(() -> service.applyToTrack(1, 9, false))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static CriteriaTemplate template() {
        CriteriaTemplate template = CriteriaTemplate.builder().id(1).name("Chuẩn").build();
        CriteriaTemplateItem item = CriteriaTemplateItem.builder()
                .id(2).template(template).name("Kỹ thuật").type(CriteriaType.TECHNICAL)
                .weight(.5f).maxScore(10).displayOrder(1).build();
        template.setItems(List.of(item));
        return template;
    }
}
