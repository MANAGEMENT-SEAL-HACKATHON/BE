package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RblCalibrationSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        createPrompts();
        createScores();
    }

    private void createPrompts() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rbl_calibration_prompts (
                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    hackathon_id INT NOT NULL,
                    round_id INT NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    description TEXT NULL,
                    sample_submission_id INT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    closed_at DATETIME(6) NULL,
                    INDEX idx_calibration_prompt_round (round_id, status),
                    CONSTRAINT fk_calibration_prompt_hackathon
                        FOREIGN KEY (hackathon_id) REFERENCES hackathons(id) ON DELETE CASCADE,
                    CONSTRAINT fk_calibration_prompt_round
                        FOREIGN KEY (round_id) REFERENCES rounds(id) ON DELETE CASCADE,
                    CONSTRAINT fk_calibration_prompt_submission
                        FOREIGN KEY (sample_submission_id) REFERENCES submissions(id) ON DELETE SET NULL
                ) ENGINE=InnoDB
                """);
        log.info("[RblCalibrationSchemaMigration] calibration prompts ready");
    }

    private void createScores() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS rbl_calibration_scores (
                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    prompt_id INT NOT NULL,
                    judge_id INT NOT NULL,
                    criterion_id INT NOT NULL,
                    score_value FLOAT NOT NULL,
                    comment TEXT NULL,
                    scored_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    CONSTRAINT uk_calibration_score UNIQUE (prompt_id, judge_id, criterion_id),
                    CONSTRAINT fk_calibration_score_prompt
                        FOREIGN KEY (prompt_id) REFERENCES rbl_calibration_prompts(id) ON DELETE CASCADE,
                    CONSTRAINT fk_calibration_score_judge
                        FOREIGN KEY (judge_id) REFERENCES users(id) ON DELETE CASCADE,
                    CONSTRAINT fk_calibration_score_criterion
                        FOREIGN KEY (criterion_id) REFERENCES criteria(id) ON DELETE CASCADE
                ) ENGINE=InnoDB
                """);
        log.info("[RblCalibrationSchemaMigration] calibration scores ready");
    }
}
