#!/bin/sh

set -eu

encode_config_value() {
    printf '%s' "$1" | base64 | tr -d '\n'
}

export ADMIN_IAM_URL_B64="$(encode_config_value "${MACULA_CLOUD_IAM_URL:?MACULA_CLOUD_IAM_URL is required}")"
export ADMIN_OAUTH_CLIENT_ID_B64="$(encode_config_value "${VITE_APP_OAUTH_CLIENT_ID:?VITE_APP_OAUTH_CLIENT_ID is required}")"
export ADMIN_OAUTH_CLIENT_SECRET_B64="$(encode_config_value "${VITE_APP_OAUTH_CLIENT_SECRET:?VITE_APP_OAUTH_CLIENT_SECRET is required}")"
export ADMIN_OAUTH_SCOPE_B64="$(encode_config_value "${VITE_APP_OAUTH_SCOPE:?VITE_APP_OAUTH_SCOPE is required}")"
export ADMIN_DEMO_USERNAME_B64="$(encode_config_value "${VITE_APP_DEMO_USERNAME:?VITE_APP_DEMO_USERNAME is required}")"
export ADMIN_DEMO_PASSWORD_B64="$(encode_config_value "${VITE_APP_DEMO_PASSWORD:?VITE_APP_DEMO_PASSWORD is required}")"

template=/opt/admin/config.js.template
target=/usr/share/nginx/html/config.js
temporary="${target}.tmp"
variables='${ADMIN_IAM_URL_B64} ${ADMIN_OAUTH_CLIENT_ID_B64} ${ADMIN_OAUTH_CLIENT_SECRET_B64} ${ADMIN_OAUTH_SCOPE_B64} ${ADMIN_DEMO_USERNAME_B64} ${ADMIN_DEMO_PASSWORD_B64}'

envsubst "$variables" < "$template" > "$temporary"
mv "$temporary" "$target"
