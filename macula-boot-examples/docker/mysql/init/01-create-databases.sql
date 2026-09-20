-- Local example bootstrap for MySQL 8.0.43.
-- The MySQL image creates nacos_config and the nacos user from MYSQL_* variables.
-- Polaris v1.18.1 uses the local-only root password from docker-compose.yml.

CREATE DATABASE IF NOT EXISTS `nacos_config`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `polaris_server`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
