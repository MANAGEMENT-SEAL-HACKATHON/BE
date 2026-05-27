package com.sealhackathon.api.auth.service.social;

public record SocialIdentity(
        String provider,
        String providerUid,
        String email,
        String displayName
) {
}
