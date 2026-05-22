package com.sealhackathon.api.tracks.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FR-03 POST /api/v1/rounds/{roundId}/tracks
 *
 * <p>Các bảng đấu trong cùng vòng thi <b>song song</b>. {@code sequenceOrder} tùy chọn — không gửi
 * thì BE tự gán {@code max(sequence_order)+1} trong round (không phải thứ tự thi giữa các vòng).
 *
 * <p>Cross-field validate {@code maxTeamSize >= minTeamSize} và {@code maxTeamsPerGroup <= maxTeams}
 * thực hiện ở service (422 nếu vi phạm).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTrackRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    private String topic;

    @Min(1)
    private Integer sequenceOrder;

    @Min(1)
    private Integer maxTeams;

    /**
     * FIX-02 v2.1 — số đội tối đa mỗi bảng đấu.
     */
    @Min(1)
    private Integer maxTeamsPerGroup;

    @NotNull
    @Min(1)
    private Integer minTeamSize;

    @NotNull
    @Min(1)
    private Integer maxTeamSize;
}
