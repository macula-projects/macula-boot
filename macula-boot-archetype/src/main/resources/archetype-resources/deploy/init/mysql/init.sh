#!/bin/sh
set -eu

mysql_host=${MYSQL_HOST:-mysql}
root_password=${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}
app_user=${MYSQL_APP_USER:?MYSQL_APP_USER is required}
app_password=${MYSQL_APP_PASSWORD:?MYSQL_APP_PASSWORD is required}
app_database=${MYSQL_APP_DATABASE:?MYSQL_APP_DATABASE is required}

validate_identifier() {
  case "$2" in
    ''|*[!A-Za-z0-9_-]*)
      echo "$1 may contain only letters, digits, underscores, and hyphens" >&2
      exit 2
      ;;
  esac
}

escape_sql_string() {
  printf '%s' "$1" | sed "s/\\\\/\\\\\\\\/g; s/'/''/g"
}

validate_identifier MYSQL_APP_USER "$app_user"
validate_identifier MYSQL_APP_DATABASE "$app_database"
sql_app_user=$(escape_sql_string "$app_user")
sql_app_password=$(escape_sql_string "$app_password")

mysql_exec() {
  mysql --protocol=tcp -h "$mysql_host" -uroot -p"$root_password" "$@"
}

mysql_exec <<SQL
CREATE DATABASE IF NOT EXISTS nacos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS \`${app_database}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${sql_app_user}'@'%' IDENTIFIED BY '${sql_app_password}';
ALTER USER '${sql_app_user}'@'%' IDENTIFIED BY '${sql_app_password}';
GRANT ALL PRIVILEGES ON nacos.* TO '${sql_app_user}'@'%';
GRANT ALL PRIVILEGES ON \`${app_database}\`.* TO '${sql_app_user}'@'%';
GRANT SELECT ON performance_schema.user_variables_by_thread TO '${sql_app_user}'@'%';
FLUSH PRIVILEGES;
SQL

if ! mysql_exec nacos -Nse "SELECT 1 FROM information_schema.tables WHERE table_schema='nacos' AND table_name='config_info'" | grep -q 1; then
  mysql_exec nacos < /init/nacos-mysql.sql
fi
