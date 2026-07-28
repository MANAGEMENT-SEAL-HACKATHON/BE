package com.sealhackathon.api.hackathons.service;

public interface HackathonRegistrationService {

    void register(Integer hackathonId);

    void unregister(Integer hackathonId);
}
