-- UM API Center module 4: business API invocation audit log.
-- Run once against the same MySQL 8 database that contains api_definition.
-- auth.password, database credentials, original SQL errors, and response bodies must never be stored here.

CREATE TABLE api_call_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    api_id BIGINT UNSIGNED NULL,
    api_path VARCHAR(200) NOT NULL,
    username VARCHAR(100) NULL,
    client_ip VARCHAR(64) NOT NULL,
    params_summary JSON NULL,
    result VARCHAR(20) NOT NULL,
    error_code VARCHAR(50) NULL,
    record_count INT UNSIGNED NULL,
    duration_ms INT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_api_call_log_request_id UNIQUE (request_id),
    CONSTRAINT fk_api_call_log_definition
        FOREIGN KEY (api_id) REFERENCES api_definition (id) ON DELETE SET NULL,
    CONSTRAINT chk_api_call_log_result CHECK (result IN ('success', 'failed')),
    INDEX idx_api_call_log_created_at (created_at),
    INDEX idx_api_call_log_api_created_at (api_id, created_at),
    INDEX idx_api_call_log_username_created_at (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
