package com.sealhackathon.api.hackathon_registrations.service;

public interface HackathonRegistrationService {

    void register(Integer hackathonId);

    void unregister(Integer hackathonId);
}
