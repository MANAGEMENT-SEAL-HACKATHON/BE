package com.sealhackathon.api.users.support;

import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonnelAssignmentRulesTest {

    @Test
    void expandPool_whenRoleMentor_withoutAccountRoleExact() {
        assertTrue(PersonnelAssignmentRules.shouldExpandPersonnelPool(
                UserRole.MENTOR, false, false));
    }

    @Test
    void noExpand_whenAccountRoleExact() {
        assertFalse(PersonnelAssignmentRules.shouldExpandPersonnelPool(
                UserRole.MENTOR, false, true));
    }

    @Test
    void noExpand_whenRoleStudent() {
        assertFalse(PersonnelAssignmentRules.shouldExpandPersonnelPool(
                UserRole.STUDENT, false, false));
    }
}
