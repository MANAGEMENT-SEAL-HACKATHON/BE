package com.se194093.be.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Impl cho {@link DateRange}. Hỗ trợ {@link LocalDate} và {@link LocalDateTime}.
 *
 * <p>Logic:
 * <ul>
 *   <li>Nếu start hoặc end null → trả PASS (cho phép partial; user dùng @NotNull để bắt riêng).</li>
 *   <li>Nếu cả hai có giá trị → end phải &gt;= start.</li>
 *   <li>Type khác → trả FAIL với message "Unsupported date type".</li>
 * </ul>
 */
public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(DateRange annotation) {
        this.startField = annotation.start();
        this.endField = annotation.end();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }
        BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        Object startVal = bean.getPropertyValue(startField);
        Object endVal = bean.getPropertyValue(endField);
        if (startVal == null || endVal == null) {
            return true;
        }
        if (startVal instanceof LocalDate s && endVal instanceof LocalDate e) {
            return !e.isBefore(s);
        }
        if (startVal instanceof LocalDateTime s && endVal instanceof LocalDateTime e) {
            return !e.isBefore(s);
        }
        return false;
    }
}
