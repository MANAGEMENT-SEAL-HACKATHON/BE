package com.sealhackathon.api.common.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops health probe at {@code GET /} (no auth, no API envelope).
 *
 * <p>API system utilities: see {@link SystemTimeController} at {@code /api/v1/system/*}.
 */
@RestController
public class HealthCheckController {

    @GetMapping("/")
    public String healthCheck() {
        return "App is running successfully!";
    }
}
