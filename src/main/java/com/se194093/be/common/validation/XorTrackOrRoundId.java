package com.se194093.be.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = XorTrackOrRoundIdValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface XorTrackOrRoundId {

    String message() default "Phải cung cấp đúng một trong trackId hoặc roundId";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
