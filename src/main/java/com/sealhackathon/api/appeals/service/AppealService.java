package com.sealhackathon.api.appeals.service;

import com.sealhackathon.api.appeals.dto.response.AppealEvidenceUploadResponse;
import com.sealhackathon.api.me.student.dto.request.CreateAppealRequest;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AppealService {

    AppealResponse create(CreateAppealRequest request);

    List<AppealResponse> listMine();

    AppealEvidenceUploadResponse uploadEvidence(MultipartFile file);
}
