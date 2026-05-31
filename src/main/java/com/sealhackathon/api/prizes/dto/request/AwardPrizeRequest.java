package com.sealhackathon.api.prizes.dto.request;

import com.sealhackathon.api.prizes.value_object.PrizeRank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwardPrizeRequest {

    @NotNull
    private Integer roundId;

    private Integer trackId;

    @NotNull
    private Integer teamId;

    @NotBlank
    @Size(max = 200)
    private String prizeName;

    private PrizeRank prizeRank;

    @Size(max = 300)
    private String prizeValue;

    private String description;
}
