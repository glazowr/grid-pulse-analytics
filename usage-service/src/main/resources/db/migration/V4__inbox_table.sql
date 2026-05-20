CREATE TABLE `inbox` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `event_id` VARCHAR(36) NOT NULL,
    `event_type` VARCHAR(50) NOT NULL,
    `processed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `payload_hash` VARCHAR(64),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id_type` (`event_id`, `event_type`),
    INDEX `idx_processed_at` (`processed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;