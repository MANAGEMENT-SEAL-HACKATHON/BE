package com.sealhackathon.api.hackathons.service;

import com.sealhackathon.api.hackathons.dto.request.RegistrationExtensionRequest;
import com.sealhackathon.api.hackathons.dto.response.RegistrationExtensionPreviewResponse;

import java.time.LocalDateTime;

public interface HackathonRegistrationExtensionService {

    RegistrationExtensionPreviewResponse preview(Integer hackathonId, LocalDateTime newRegistrationEnd);

    RegistrationExtensionPreviewResponse extend(Integer hackathonId, RegistrationExtensionRequest request);
}
