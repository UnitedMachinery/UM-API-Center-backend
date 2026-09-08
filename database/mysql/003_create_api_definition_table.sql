-- UM API Center module 3: configurable read-only query API definitions.
-- Run once against the same MySQL 8 database that contains api_user and api_datasource.

CREATE TABLE api_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    api_path VARCHAR(200) NOT NULL,
    datasource_id BIGINT UNSIGNED NOT NULL,
    params_schema JSON NOT NULL,
    sql_text TEXT NOT NULL,
    timeout_seconds TINYINT UNSIGNED NOT NULL DEFAULT 5,
    max_rows SMALLINT UNSIGNED NOT NULL DEFAULT 500,
    is_enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_api_definition_path UNIQUE (api_path),
    CONSTRAINT fk_api_definition_datasource
        FOREIGN KEY (datasource_id) REFERENCES api_datasource (id),
    CONSTRAINT chk_api_definition_timeout
        CHECK (timeout_seconds BETWEEN 1 AND 20),
    CONSTRAINT chk_api_definition_max_rows
        CHECK (max_rows BETWEEN 1 AND 10000),
    INDEX idx_api_definition_datasource (datasource_id),
    INDEX idx_api_definition_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
