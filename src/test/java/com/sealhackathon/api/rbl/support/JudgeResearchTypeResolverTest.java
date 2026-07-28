package com.sealhackathon.api.rbl.support;

import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RBL-JUDGE-TYPE-01 */
class JudgeResearchTypeResolverTest {

    @Test
    void faculty_internalNonTempJudge() {
        User u = User.builder().id(1).role(UserRole.JUDGE)
                .userType(UserType.INTERNAL).isTempAccount(false).build();
        assertThat(JudgeResearchTypeResolver.resolve(u)).isEqualTo(JudgeResearchType.FACULTY);
    }

    @Test
    void guest_tempAccount() {
        User u = User.builder().id(2).role(UserRole.JUDGE)
                .userType(UserType.INTERNAL).isTempAccount(true).build();
        assertThat(JudgeResearchTypeResolver.resolve(u)).isEqualTo(JudgeResearchType.GUEST);
    }

    @Test
    void guest_externalUserType() {
        User u = User.builder().id(3).role(UserRole.JUDGE)
                .userType(UserType.EXTERNAL).isTempAccount(false).build();
        assertThat(JudgeResearchTypeResolver.resolve(u)).isEqualTo(JudgeResearchType.GUEST);
    }

    @Test
    void other_unspecified() {
        User u = User.builder().id(4).role(UserRole.JUDGE)
                .userType(UserType.UNSPECIFIED).isTempAccount(false).build();
        assertThat(JudgeResearchTypeResolver.resolve(u)).isEqualTo(JudgeResearchType.OTHER);
    }

    @Test
    void other_nonJudge() {
        User u = User.builder().id(5).role(UserRole.STUDENT)
                .userType(UserType.INTERNAL).isTempAccount(false).build();
        assertThat(JudgeResearchTypeResolver.resolve(u)).isEqualTo(JudgeResearchType.OTHER);
    }
}
