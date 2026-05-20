package com.sealhackathon.api.tracks.dto.request;

import com.sealhackathon.api.tracks.value_object.TrackStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTrackRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    /**
     * Chủ đề bốc thăm — workflow GĐ1: có thể cập nhật sau KICKOFF (mf01 §10 FR-03).
     */
    @Size(max = 300)
    private String topic;

    @Min(1)
    private Integer maxTeams;

    @Min(1)
    private Integer maxTeamsPerGroup;

    @NotNull
    @Min(1)
    private Integer minTeamSize;

    @NotNull
    @Min(1)
    private Integer maxTeamSize;

    /**
     * Cho phép đổi status. Nếu chuyển CANCELLED khi đang có team registered → cảnh báo mềm.
     */
    private TrackStatus status;
}
