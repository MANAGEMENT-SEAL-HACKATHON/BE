package com.sealhackathon.api.hackathons.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import org.springframework.util.StringUtils;

public final class HackathonBannerUrls {

    private HackathonBannerUrls() {
    }

    public static String publicPath(Integer hackathonId) {
        return "/api/v1/hackathons/" + hackathonId + "/banner";
    }

    public static String resolveForResponse(Hackathon hackathon) {
        if (hackathon == null || !StringUtils.hasText(hackathon.getBannerUrl())) {
            return null;
        }
        String stored = hackathon.getBannerUrl().trim();
        if (HackathonBannerStorageService.isStorageKey(stored)) {
            return publicPath(hackathon.getId());
        }
        // Legacy external URL — still returned until seed/migration replaces it.
        return stored;
    }
}
