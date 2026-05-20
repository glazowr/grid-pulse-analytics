ALTER TABLE `alert`
    ADD COLUMN `dedupe_key` VARCHAR(255) DEFAULT NULL,
    ADD COLUMN `window_start` TIMESTAMP NULL,
    ADD UNIQUE KEY `uk_dedupe_window` (`user_id`, `dedupe_key`, `window_start`);