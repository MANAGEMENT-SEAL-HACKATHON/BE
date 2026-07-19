package com.sealhackathon.api.prizes.service;

import com.sealhackathon.api.prizes.dto.request.AwardPrizeRequest;
import com.sealhackathon.api.prizes.dto.request.RevokePrizeRequest;
import com.sealhackathon.api.prizes.dto.request.UpdateAwardedPrizeRequest;
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;

import java.util.List;

public interface PrizeService {

    PrizeResponse award(Integer hackathonId, AwardPrizeRequest req);

    List<PrizeResponse> listByHackathon(Integer hackathonId);

    PrizeResponse updateAwarded(Integer prizeId, UpdateAwardedPrizeRequest req);

    void revoke(Integer prizeId, RevokePrizeRequest req);
}
