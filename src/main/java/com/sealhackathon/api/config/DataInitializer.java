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
 * <p>Thứ tự startup: {@link RoundExamAtSchemaMigration} (0) → {@link CriteriaCloneSourceUnlinkMigration} (1)
 * → DataInitializer (2).
 *
 * <p>Mỗi lần start: repair timeline (đăng ký 24/05–05/06, WS 06/06, KO 07/06, thi 10/06),
 * repair/bổ sung hackathon {@link Gd1SeedConstants#SLUG_FINISHED} (dataset archive đầy đủ),
 * repair criteria/track seed (gỡ {@code source_criteria_id} cũ, bổ sung Track 3 trống để test clone 2→3).
 *
 * <p>Tham chiếu: {@code docs/mf01/02-functional-requirements.md} §11.1, {@code docs/api/fe-round-exam-at-migration.md},
 * {@code docs/api/fe-criteria-clone.md}.
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
        gd1DataSeeder.repairSeededTimeline();
        gd1DataSeeder.repairSeededFinishedHackathon();
        gd1DataSeeder.repairSeededCriteriaAndTracks();
        gd1DataSeeder.repairDevUserPasswords();

        if (gd1DataSeeder.isAlreadySeeded()) {
            log.info("[DataInitializer] Seed đã có (slug={}), bỏ qua tạo mới.",
                    Gd1SeedConstants.SLUG_ONGOING);
            return;
        }
        gd1DataSeeder.seedAll();
    }
}
