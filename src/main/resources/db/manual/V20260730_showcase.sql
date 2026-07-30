-- Phase 8 — Hall of Fame + Showcase articles (block-based, no markdown)

CREATE TABLE IF NOT EXISTS hall_of_fame_entries (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id     INT          NOT NULL,
    hackathon_name   VARCHAR(300) NOT NULL,
    year             INT          NOT NULL,
    season           VARCHAR(20)  NOT NULL,
    team_id          INT          NULL,
    team_name        VARCHAR(200) NOT NULL,
    member_names     TEXT         NULL,
    track_name       VARCHAR(200) NULL,
    prize_name       VARCHAR(200) NULL,
    prize_value      VARCHAR(300) NULL,
    awarded_at       DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hof_hackathon UNIQUE (hackathon_id),
    CONSTRAINT fk_hof_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id),
    CONSTRAINT fk_hof_team FOREIGN KEY (team_id) REFERENCES teams (id)
);

CREATE TABLE IF NOT EXISTS showcase_articles (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id     INT          NULL,
    slug             VARCHAR(180) NOT NULL,
    title            VARCHAR(300) NOT NULL,
    summary          TEXT         NULL,
    cover_image_key  VARCHAR(512) NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at     DATETIME     NULL,
    author_id        INT          NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_showcase_articles_slug UNIQUE (slug),
    CONSTRAINT fk_showcase_articles_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id),
    CONSTRAINT fk_showcase_articles_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS showcase_article_blocks (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    article_id  INT         NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    type        VARCHAR(20) NOT NULL,
    text        TEXT        NULL,
    image_key   VARCHAR(512) NULL,
    CONSTRAINT fk_showcase_blocks_article FOREIGN KEY (article_id) REFERENCES showcase_articles (id) ON DELETE CASCADE
);

CREATE INDEX idx_showcase_articles_status ON showcase_articles (status);
CREATE INDEX idx_showcase_articles_hackathon ON showcase_articles (hackathon_id);
CREATE INDEX idx_hof_year ON hall_of_fame_entries (year);
CREATE INDEX idx_showcase_blocks_article ON showcase_article_blocks (article_id, sort_order);
