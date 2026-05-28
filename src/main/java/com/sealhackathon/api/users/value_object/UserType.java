package com.sealhackathon.api.users.value_object;

public enum UserType {
    INTERNAL,
    EXTERNAL,
    /**
     * Dùng cho social login lần đầu khi user chưa khai báo hệ INTERNAL/EXTERNAL.
     */
    UNSPECIFIED
}