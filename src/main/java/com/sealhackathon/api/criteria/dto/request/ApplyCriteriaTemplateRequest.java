package com.sealhackathon.api.criteria.dto.request;

public record ApplyCriteriaTemplateRequest(Boolean replaceExisting) {
    public boolean replace() {
        return Boolean.TRUE.equals(replaceExisting);
    }
}
