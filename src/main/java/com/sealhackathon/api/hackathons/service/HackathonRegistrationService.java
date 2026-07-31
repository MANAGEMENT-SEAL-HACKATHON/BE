package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.dto.request.RegisterHackathonRequest;

public interface HackathonRegistrationService {

    void register(Integer hackathonId);

    void register(Integer hackathonId, RegisterHackathonRequest request);

    void unregister(Integer hackathonId);
}
