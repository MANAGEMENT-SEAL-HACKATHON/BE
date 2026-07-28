package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.dto.request.CloseRegistrationEarlyRequest;
import com.sealhackathon.api.hackathons.dto.response.CloseRegistrationEarlyResponse;

public interface HackathonRegistrationCloseService {

    CloseRegistrationEarlyResponse closeRegistrationEarly(Integer hackathonId,
                                                          CloseRegistrationEarlyRequest request);
}
