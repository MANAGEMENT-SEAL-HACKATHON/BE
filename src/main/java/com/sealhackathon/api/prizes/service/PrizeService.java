package com.sealhackathon.api.prizes.service;

import com.sealhackathon.api.prizes.dto.request.AwardPrizeRequest;
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;

import java.util.List;

public interface PrizeService {

    PrizeResponse award(Integer hackathonId, AwardPrizeRequest req);

    List<PrizeResponse> listByHackathon(Integer hackathonId);

    void revoke(Integer prizeId);
}
