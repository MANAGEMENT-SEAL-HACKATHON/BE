package com.sealhackathon.api.common.validation;

import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class XorTrackOrRoundIdValidator implements ConstraintValidator<XorTrackOrRoundId, CreateJudgeAssignmentRequest> {

    @Override
    public boolean isValid(CreateJudgeAssignmentRequest req, ConstraintValidatorContext context) {
        if (req == null) {
            return true;
        }
        boolean hasTrack = req.getTrackId() != null;
        boolean hasRound = req.getRoundId() != null;
        return hasTrack ^ hasRound;
    }
}
