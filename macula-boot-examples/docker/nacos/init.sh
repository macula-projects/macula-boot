#!/bin/sh
# 使用 Nacos 3 Console API 幂等初始化本地示例，不覆盖已有配置。
set -eu

nacos_base_url=${NACOS_BASE_URL:-http://nacos:8080}
nacos_namespace=${NACOS_NAMESPACE:-MACULA5}

# Server 端口就绪不代表 Console 与数据库已可用，最多检查 60 次。
attempt=0
while :; do
  readiness=$(curl --fail --silent --show-error --max-time 5 "${nacos_base_url}/v3/console/health/readiness" 2>/dev/null || true)
  if printf '%s' "$readiness" | grep -Eq '"data"[[:space:]]*:[[:space:]]*"ok"'; then
    break
  fi
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo 'Nacos Console did not become ready' >&2
    exit 1
  fi
  sleep 2
done

for namespace in "$nacos_namespace" SENTINEL; do
  namespaces=$(curl --fail --silent --show-error --max-time 10 "${nacos_base_url}/v3/console/core/namespace/list")
  printf '%s' "$namespaces" | grep -Eq '"code"[[:space:]]*:[[:space:]]*0[[:space:]]*[,}]'
  if ! printf '%s' "$namespaces" | grep -Fq "\"namespace\":\"${namespace}\""; then
    response=$(curl --fail --silent --show-error --max-time 10 --request POST \
      --data-urlencode "customNamespaceId=${namespace}" \
      --data-urlencode "namespaceName=${namespace}" \
      --data-urlencode 'namespaceDesc=Macula Boot local example' \
      "${nacos_base_url}/v3/console/core/namespace")
    printf '%s' "$response" | grep -Eq '"data"[[:space:]]*:[[:space:]]*true'
  fi
done

data_id=macula-example-alibaba-provider1-local.yml
existing=$(curl --fail --silent --show-error --max-time 10 --get \
  --data-urlencode "dataId=${data_id}" \
  --data-urlencode 'groupName=DEFAULT_GROUP' \
  --data-urlencode "namespaceId=${nacos_namespace}" \
  "${nacos_base_url}/v3/console/cs/config")
# API 业务错误也必须失败，不能误判为配置不存在后覆盖数据。
printf '%s' "$existing" | grep -Eq '"code"[[:space:]]*:[[:space:]]*0[[:space:]]*[,}]'
if ! printf '%s' "$existing" | grep -Fq "\"dataId\":\"${data_id}\""; then
  printf '%s' "$existing" | grep -Eq '"data"[[:space:]]*:[[:space:]]*null[[:space:]]*[,}]'
  response=$(curl --fail --silent --show-error --max-time 10 --request POST \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode 'groupName=DEFAULT_GROUP' \
    --data-urlencode "namespaceId=${nacos_namespace}" \
    --data-urlencode 'type=yaml' \
    --data-urlencode 'content=example:
  test: nacos-initial' \
    "${nacos_base_url}/v3/console/cs/config")
  printf '%s' "$response" | grep -Eq '"data"[[:space:]]*:[[:space:]]*true'
fi
echo 'Nacos initialization completed'
