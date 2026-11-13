#!/bin/sh
# 与 macula-cloud/deploy/init/mysql/init.sh 对齐，仅补建缺失的本地 Nacos 用户和角色。
set -eu

mysql_host=${MYSQL_HOST:-mysql}
mysql_database=${MYSQL_DATABASE:-nacos_config}
mysql_root_password=${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}

# 密码只通过客户端环境传递；数据库名作为 CLI 参数，不拼入 SQL。
# 使用独立服务而非首次启动目录，使已有卷也能执行；INSERT IGNORE 保留已有密码。
MYSQL_PWD="$mysql_root_password" mysql --protocol=TCP --connect-timeout=10 \
  --host="$mysql_host" --user=root --database="$mysql_database" <<'SQL'
INSERT IGNORE INTO users (username, password, enabled)
VALUES ('nacos', '$2a$10$naftHYJp6.QvSL.bQyhESOA1yD/U1hYsYMtFPprh3DCVPu13d.l2a', TRUE);
INSERT IGNORE INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN');
SQL

echo 'Nacos local user initialization completed; existing credentials preserved'
