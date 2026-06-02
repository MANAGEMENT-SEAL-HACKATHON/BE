package com.sealhackathon.api.individual_rankings.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.individual_rankings.dto.response.IndividualRankingItemResponse;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IndividualRankingServiceImpl implements IndividualRankingService {

    private final IndividualRankingRepository individualRankingRepository;
    private final HackathonRepository hackathonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IndividualRankingItemResponse> listByHackathon(Integer hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        // Rào chắn 1: Kiểm tra cấu hình mùa giải có bật xếp hạng cá nhân không
        if (!Boolean.TRUE.equals(hackathon.getIndividualRankingEnabled())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Hackathon này không kích hoạt chế độ Xếp hạng Cá nhân.");
        }

        // Rào chắn 2: Chỉ public khi Hackathon đã FINISHED
        if (hackathon.getStatus() != HackathonStatus.FINISHED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE,
                    "Bảng xếp hạng Cá nhân chỉ được công bố khi Hackathon đã kết thúc.");
        }

        return individualRankingRepository.findByHackathon_IdOrderByRankAsc(hackathonId)
                .stream()
                .map(ir -> IndividualRankingItemResponse.builder()
                        .userId(ir.getUser().getId())
                        .fullName(ir.getUser().getFullName())
                        .scoreThisHackathon(ir.getScoreThisHackathon())
                        .cumulativeScore(ir.getCumulativeScore())
                        .rank(ir.getRank())
                        .build())
                .toList();
    }

    @Override
    public void calculateAsync(Integer hackathonId) {
        log.info("[IndividualRanking] Nhận tín hiệu tính toán xếp hạng Cá nhân cho Hackathon ID: {}", hackathonId);
        // TODO: (Worker) Logic tính điểm cá nhân dựa trên đóng góp, commit hoặc điểm đội,
        // sau đó cộng dồn vào cumulative_score. Bỏ qua nếu individual_ranking_enabled = false.
    }
}