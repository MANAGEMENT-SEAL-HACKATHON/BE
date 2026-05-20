package com.sealhackathon.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean validation annotation: kiểm tra một cặp field date theo thứ tự {@code start &lt;= end}.
 *
 * <p>Dùng trên class DTO. Ví dụ:
 * <pre>{@code
 * @DateRange(start = "registrationStart", end = "registrationEnd",
 *            message = "registrationEnd phải >= registrationStart")
 * @DateRange(start = "eventStart", end = "eventEnd",
 *            message = "eventEnd phải >= eventStart")
 * public class CreateHackathonRequest { ... }
 * }</pre>
 *
 * <p>Bỏ qua nếu một trong hai field null (cho phép payload partial). Logic strict bắt buộc
 * not-null thì kết hợp với {@code @NotNull} ở field tương ứng.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@java.lang.annotation.Repeatable(DateRange.List.class)
public @interface DateRange {

    String start();

    String end();

    String message() default "End date must be on/after start date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        DateRange[] value();
    }
}
