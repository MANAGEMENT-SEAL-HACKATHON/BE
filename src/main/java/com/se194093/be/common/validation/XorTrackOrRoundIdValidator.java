package com.se194093.be.common.validation;

import com.se194093.be.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
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
