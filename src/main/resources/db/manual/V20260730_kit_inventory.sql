-- Hibernate ddl-auto=update also applies these on entity change;
-- run manually when ddl-auto is off.

ALTER TABLE hackathon_registrations
    ADD COLUMN preferred_shirt_size VARCHAR(10) NULL;

CREATE TABLE IF NOT EXISTS kit_items (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id  INT          NOT NULL,
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    has_size      TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_kit_items_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id)
);

CREATE TABLE IF NOT EXISTS kit_stocks (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    kit_item_id      INT         NOT NULL,
    size             VARCHAR(10) NULL,
    size_key         VARCHAR(10) NOT NULL DEFAULT '',
    quantity_total   INT         NOT NULL DEFAULT 0,
    quantity_issued  INT         NOT NULL DEFAULT 0,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_kit_stocks_item FOREIGN KEY (kit_item_id) REFERENCES kit_items (id),
    CONSTRAINT uk_kit_stock_item_size UNIQUE (kit_item_id, size_key)
);

CREATE TABLE IF NOT EXISTS kit_allocations (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    hackathon_id  INT          NOT NULL,
    user_id       INT          NOT NULL,
    kit_item_id   INT          NOT NULL,
    size          VARCHAR(10)  NULL,
    status        VARCHAR(20)  NOT NULL,
    issued_at     DATETIME     NULL,
    issued_by     INT          NULL,
    note          VARCHAR(1000) NULL,
    CONSTRAINT fk_kit_alloc_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id),
    CONSTRAINT fk_kit_alloc_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_kit_alloc_item FOREIGN KEY (kit_item_id) REFERENCES kit_items (id),
    CONSTRAINT fk_kit_alloc_issuer FOREIGN KEY (issued_by) REFERENCES users (id),
    CONSTRAINT uk_kit_alloc_hackathon_user_item UNIQUE (hackathon_id, user_id, kit_item_id)
);
