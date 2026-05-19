package com.sealhackathon.api.config;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Hibernate 7 {@link FormatMapper} dùng Jackson 3 ({@code tools.jackson.databind}) thay cho Jackson 2
 * mà bundle mặc định của Hibernate yêu cầu.
 *
 * <p><b>Lý do tồn tại:</b><br>
 * Spring Boot 4 ship Jackson 3 (package {@code tools.jackson.*}) thay cho Jackson 2
 * ({@code com.fasterxml.jackson.*}). Hibernate 7's
 * {@code org.hibernate.type.format.jackson.JacksonJsonFormatMapper} chỉ biết Jackson 2 và auto-discovery
 * thất bại với Jackson 3 → throw {@code HibernateException: Could not find a FormatMapper for the JSON
 * format} khi persist entity có field {@code @JdbcTypeCode(SqlTypes.JSON)} (vd {@code AuditLog.detail}).
 *
 * <p><b>Cách activate:</b> đăng ký qua Hibernate property trong {@code application.properties}:
 * <pre>{@code
 * spring.jpa.properties.hibernate.type.json_format_mapper=com.se194093.be.config.Jackson3JsonFormatMapper
 * }</pre>
 * Hibernate sẽ instantiate qua no-arg constructor.
 */
public class Jackson3JsonFormatMapper implements FormatMapper {

    private final ObjectMapper objectMapper;

    public Jackson3JsonFormatMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public Jackson3JsonFormatMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType,
                            WrapperOptions wrapperOptions) {
        if (charSequence == null) {
            return null;
        }
        String raw = charSequence.toString();
        try {
            Class<T> targetClass = javaType.getJavaTypeClass();
            if (targetClass != null && JsonNode.class.isAssignableFrom(targetClass)) {
                return (T) objectMapper.readTree(raw);
            }
            return objectMapper.readValue(raw, targetClass);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "Jackson3JsonFormatMapper: cannot deserialize JSON to "
                    + javaType.getJavaType().getTypeName(), e);
        }
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "Jackson3JsonFormatMapper: cannot serialize value of type "
                    + value.getClass().getName() + " to JSON", e);
        }
    }
}
