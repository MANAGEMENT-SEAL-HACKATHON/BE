package com.sealhackathon.api.chapter_rankings.service.impl;

import com.sealhackathon.api.chapter_rankings.dto.response.ChapterRankingItemResponse;
import com.sealhackathon.api.chapter_rankings.repository.ChapterRankingRepository;
import com.sealhackathon.api.chapter_rankings.service.ChapterRankingService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChapterRankingServiceImpl implements ChapterRankingService {

    private final ChapterRankingRepository chapterRankingRepository;
    private final HackathonRepository hackathonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChapterRankingItemResponse> listByHackathon(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        // Rào chắn: Chỉ public Bảng xếp hạng khi Hackathon đã chính thức FINISHED
        if (hackathon.getStatus() != HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Bảng xếp hạng Cơ sở (Chapter) chỉ được công bố khi Hackathon đã kết thúc.");
        }

        return chapterRankingRepository.findByHackathon_IdOrderByRankAsc(hackathonId)
                .stream()
                .map(cr -> ChapterRankingItemResponse.builder()
                        .chapterId(cr.getChapter().getId())
                        .chapterName(cr.getChapter().getName())
                        .bestTeamScore(cr.getBestTeamScore())
                        .totalScore(cr.getTotalScore())
                        .rank(cr.getRank())
                        .teamsParticipated(cr.getTeamsParticipated())
                        .prizesWon(cr.getPrizesWon())
                        .build())
                .toList();
    }

    @Override
    public void calculateAsync(Integer hackathonId) {
        log.info("[ChapterRanking] Nhận tín hiệu tính toán xếp hạng Cơ sở cho Hackathon ID: {}", hackathonId);
        // TODO: (Worker) Logic gom nhóm điểm (Sum/Avg final_score) của các đội theo chapter_id
        // và lưu vào bảng chapter_rankings. Quá trình này thường được gọi thông qua Message Queue
        // ngay sau khi HackathonClosureService chuyển trạng thái sang FINISHED.
    }
}