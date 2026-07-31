-- Phase 13: Kit bundle + shirt fit (UNISEX default), stock keyed by (fit, size).
-- Hibernate ddl-auto=update may also apply entity changes; run manually when ddl-auto is off.
--
-- Order matters: add columns → backfill SHIRT fit → drop old unique → add new unique.

-- 1) kit_stocks: fit + fit_key
ALTER TABLE kit_stocks
    ADD COLUMN fit VARCHAR(20) NULL,
    ADD COLUMN fit_key VARCHAR(20) NOT NULL DEFAULT '';

-- 2) Backfill UNISEX for SHIRT item stocks
UPDATE kit_stocks ks
    INNER JOIN kit_items ki ON ki.id = ks.kit_item_id
SET ks.fit = 'UNISEX',
    ks.fit_key = 'UNISEX'
WHERE ki.type = 'SHIRT'
  AND (ks.fit_key IS NULL OR ks.fit_key = '');

-- 3) Drop old unique (kit_item_id, size_key)
ALTER TABLE kit_stocks DROP INDEX uk_kit_stock_item_size;

-- 4) New unique (kit_item_id, fit_key, size_key)
ALTER TABLE kit_stocks
    ADD CONSTRAINT uk_kit_stock_item_fit_size UNIQUE (kit_item_id, fit_key, size_key);

-- 5) kit_allocations.fit
ALTER TABLE kit_allocations
    ADD COLUMN fit VARCHAR(20) NULL;

-- 6) hackathon_registrations.preferred_shirt_fit
ALTER TABLE hackathon_registrations
    ADD COLUMN preferred_shirt_fit VARCHAR(20) NULL;

-- 7) Combo tables
CREATE TABLE IF NOT EXISTS kit_bundles (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hackathon_id  INT          NOT NULL,
    name          VARCHAR(200) NOT NULL,
    is_default    TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_kit_bundles_hackathon FOREIGN KEY (hackathon_id) REFERENCES hackathons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kit_bundle_items (
    id           INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bundle_id    INT NOT NULL,
    kit_item_id  INT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_kit_bundle_items_bundle FOREIGN KEY (bundle_id) REFERENCES kit_bundles (id) ON DELETE CASCADE,
    CONSTRAINT fk_kit_bundle_items_item FOREIGN KEY (kit_item_id) REFERENCES kit_items (id),
    CONSTRAINT uk_kit_bundle_item UNIQUE (bundle_id, kit_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
