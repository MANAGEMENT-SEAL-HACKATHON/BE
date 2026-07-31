ALTER TABLE hackathons MODIFY COLUMN registration_start DATETIME NULL;
ALTER TABLE hackathons MODIFY COLUMN registration_end   DATETIME NULL;
UPDATE hackathons
   SET registration_start = DATE(registration_start)
 WHERE registration_start IS NOT NULL;
UPDATE hackathons
   SET registration_end = DATE_ADD(DATE(registration_end), INTERVAL '23:59' HOUR_MINUTE)
 WHERE registration_end IS NOT NULL;
