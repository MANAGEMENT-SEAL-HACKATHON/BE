package com.sealhackathon.api.prizes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thu hồi giải — ngang chuẩn Wildcard Override: category + note bắt buộc.
 * Categories: AWARDED_IN_ERROR | TEAM_DQ | DUPLICATE_AWARD | OTHER
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokePrizeRequest {

    @NotBlank
    @Size(max = 64)
    private String category;

    @NotBlank
    @Size(max = 1000)
    private String note;
}
