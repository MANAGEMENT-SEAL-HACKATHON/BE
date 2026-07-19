package com.sealhackathon.api.rbl.support;

import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapping khóa RQ3 (ổn định theo user):
 * <pre>
 * GUEST   = role JUDGE AND (isTempAccount OR userType == EXTERNAL)
 * FACULTY = role JUDGE AND userType == INTERNAL AND !isTempAccount
 * OTHER   = còn lại
 * </pre>
 */
@Slf4j
public final class JudgeResearchTypeResolver {

    private JudgeResearchTypeResolver() {}

    public static JudgeResearchType resolve(User judge) {
        if (judge == null) {
            return JudgeResearchType.OTHER;
        }
        if (judge.getRole() != UserRole.JUDGE) {
            log.warn("[RBL] judge id={} role={} is not JUDGE — classified OTHER",
                    judge.getId(), judge.getRole());
            return JudgeResearchType.OTHER;
        }
        boolean temp = Boolean.TRUE.equals(judge.getIsTempAccount());
        UserType type = judge.getUserType();
        if (temp || type == UserType.EXTERNAL) {
            return JudgeResearchType.GUEST;
        }
        if (type == UserType.INTERNAL && !temp) {
            return JudgeResearchType.FACULTY;
        }
        log.warn("[RBL] judge id={} userType={} isTemp={} — classified OTHER (excluded from RQ3)",
                judge.getId(), type, temp);
        return JudgeResearchType.OTHER;
    }
}
