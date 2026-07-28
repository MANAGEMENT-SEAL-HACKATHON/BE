package com.sealhackathon.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class CriteriaTemplateSchemaMigration implements CommandLineRunner {
    private static final String DEFAULT_NAME = "Bộ tiêu chí chuẩn";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS criteria_templates (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    description TEXT NULL,
                    is_default TINYINT(1) NOT NULL DEFAULT 0,
                    created_by INT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_criteria_templates_created_by
                        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS criteria_template_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    template_id INT NOT NULL,
                    name VARCHAR(200) NOT NULL,
                    type ENUM('TECHNICAL','SOFT_SKILL','PENALTY') NOT NULL,
                    weight FLOAT NOT NULL,
                    max_score INT NOT NULL,
                    description TEXT NULL,
                    display_order INT NOT NULL,
                    CONSTRAINT fk_criteria_template_items_template
                        FOREIGN KEY (template_id) REFERENCES criteria_templates(id) ON DELETE CASCADE
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO criteria_templates(name, description, is_default, created_at, updated_at)
                SELECT ?, 'Mẫu mặc định đồng bộ với STANDARD_SYSTEM_CRITERIA trên giao diện', 1,
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                 WHERE NOT EXISTS (SELECT 1 FROM criteria_templates WHERE is_default = 1)
                """, DEFAULT_NAME);
        Integer templateId = jdbcTemplate.queryForObject(
                "SELECT id FROM criteria_templates WHERE is_default = 1 ORDER BY id LIMIT 1", Integer.class);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM criteria_template_items WHERE template_id = ?", Integer.class, templateId);
        if (count != null && count == 0) {
            insert(templateId, "Chất lượng giải pháp", "TECHNICAL", .30f,
                    "Mức độ hoàn thiện, sáng tạo và phù hợp của sản phẩm.", 1);
            insert(templateId, "Tính khả thi kỹ thuật", "TECHNICAL", .25f,
                    "Kiến trúc, triển khai và độ ổn định của hệ thống.", 2);
            insert(templateId, "Trình bày & demo", "SOFT_SKILL", .25f,
                    "Khả năng truyền đạt ý tưởng và demo sản phẩm.", 3);
            insert(templateId, "Làm việc nhóm", "SOFT_SKILL", .20f,
                    "Phối hợp, phân công và đóng góp của thành viên.", 4);
        }
    }

    private void insert(Integer templateId, String name, String type, float weight,
                        String description, int displayOrder) {
        jdbcTemplate.update("""
                INSERT INTO criteria_template_items
                    (template_id, name, type, weight, max_score, description, display_order)
                VALUES (?, ?, ?, ?, 10, ?, ?)
                """, templateId, name, type, weight, description, displayOrder);
    }
}
