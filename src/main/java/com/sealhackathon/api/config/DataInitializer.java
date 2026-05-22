package com.sealhackathon.api.config;

import com.sealhackathon.api.config.seed.Gd1DataSeeder;
import com.sealhackathon.api.config.seed.Gd1SeedConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dev profile: seed MF-01 Giai đoạn 1 (chapters → users → hackathons → …).
 *
 * <p>Chạy sau {@link RoundExamAtSchemaMigration} (Order 0) để cột {@code rounds.exam_at} đã backfill NOT NULL.
 * Tham chiếu: {@code docs/workflow/mf01.md} §11.1, {@code docs/api/fe-round-exam-at-migration.md}.
 */
@Slf4j
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final Gd1DataSeeder gd1DataSeeder;

    @Override
    public void run(String... args) {
        gd1DataSeeder.repairSeededRoundsExamAt();

        if (gd1DataSeeder.isAlreadySeeded()) {
            log.info("[DataInitializer] Seed đã có (slug={}), bỏ qua tạo mới.",
                    Gd1SeedConstants.SLUG_ONGOING);
            return;
        }
        gd1DataSeeder.seedAll();
    }
}
