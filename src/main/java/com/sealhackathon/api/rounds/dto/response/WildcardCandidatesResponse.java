package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardCandidatesResponse {

    private boolean hackathonWildcardEnabled;
    private boolean roundWildcardEnabled;
    /** Số suất vé vớt cần bù (minTeamsFinal − đội đậu Top N mỗi bảng). */
    private int availableSlots;
    private int autoAdvancedCount;
    private int approvedCount;
    /** true khi mọi ứng viên trong pool đã có quyết định (duyệt/từ chối). */
    private boolean decisionsFinalized;
    /** Plan C — null nếu chưa xác nhận đề xuất. */
    private LocalDateTime proposalConfirmedAt;
    private List<WildcardCandidateResponse> candidates;
}
