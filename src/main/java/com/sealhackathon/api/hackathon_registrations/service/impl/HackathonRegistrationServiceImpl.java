package com.sealhackathon.api.hackathon_registrations.service.impl;

import com.sealhackathon.api.hackathon_registrations.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathon_registrations.service.HackathonRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HackathonRegistrationServiceImpl implements HackathonRegistrationService {

    private final HackathonRegistrationRepository hackathonRegistrationRepository;

    @Override
    public void register(Integer hackathonId) {
        // TODO: FR-U-06 — validate window, INSERT hackathon_registrations
    }

    @Override
    public void unregister(Integer hackathonId) {
        // TODO: FR-U-06 — DELETE if no active team
    }
}
