-- Phase 12: Buffet as EventType.BUFFET in prelim–final break window.
-- Hibernate ddl-auto=update may also apply entity changes; run manually when ddl-auto is off.
--
-- NOTE: MySQL CHECK constraint name may vary (chk_events_type). If DROP fails, inspect:
--   SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'events' AND CONSTRAINT_TYPE = 'CHECK';

-- 1) Drop Kickoff-only buffet columns (added in V20260730_event_buffet.sql)
ALTER TABLE events DROP COLUMN IF EXISTS buffet_location;
ALTER TABLE events DROP COLUMN IF EXISTS buffet_starts_at;
ALTER TABLE events DROP COLUMN IF EXISTS buffet_ends_at;

-- 2) Buffet menu items (FK → events)
CREATE TABLE IF NOT EXISTS buffet_menu_items (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id      INT          NOT NULL,
    name          VARCHAR(200) NOT NULL,
    quantity      INT          NOT NULL,
    unit          VARCHAR(30)  NULL,
    note          VARCHAR(500) NULL,
    display_order INT          NULL,
    CONSTRAINT fk_buffet_menu_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Expand events.type CHECK to include BUFFET
-- Defensive: drop known name; recreate with BUFFET.
ALTER TABLE events DROP CHECK chk_events_type;
ALTER TABLE events
    ADD CONSTRAINT chk_events_type
    CHECK (type IN ('KICKOFF','WORKSHOP','PRESENTATION','AWARDS','BUFFET','OTHER'));
