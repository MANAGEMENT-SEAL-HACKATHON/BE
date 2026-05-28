package com.sealhackathon.api.prizes.service;

import com.sealhackathon.api.prizes.dto.request.AwardPrizeRequest;
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;

public interface PrizeService {

    PrizeResponse award(Integer hackathonId, AwardPrizeRequest req);
}
