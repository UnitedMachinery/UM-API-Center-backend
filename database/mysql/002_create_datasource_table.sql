-- UM API Center module 2: managed JDBC data sources.
-- Run once against the same MySQL 8 database that contains api_user.
-- db_type is intentionally not a database ENUM/CHECK constraint: adding a new
-- supported JDBC provider must not require a schema migration.

CREATE TABLE api_datasource (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    db_type VARCHAR(30) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT UNSIGNED NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_encrypted TEXT NOT NULL,
    connection_options JSON NULL,
    is_enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(500) NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(100) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_api_datasource_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
