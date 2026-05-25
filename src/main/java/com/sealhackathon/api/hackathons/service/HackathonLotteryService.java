package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.dto.request.HackathonLotteryRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonLotteryResponse;

public interface HackathonLotteryService {

    HackathonLotteryResponse runLottery(Integer hackathonId, HackathonLotteryRequest req);
}
