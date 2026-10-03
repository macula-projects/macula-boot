#!/bin/sh
set -eu

nacos_base_url=${NACOS_BASE_URL:-http://nacos:8080}
namespace=${NACOS_NAMESPACE:-MACULA5}

curl --fail --silent --show-error --request POST \
  --data-urlencode "customNamespaceId=$namespace" \
  --data-urlencode "namespaceName=$namespace" \
  --data-urlencode "namespaceDesc=Generated project local development" \
  "$nacos_base_url/v3/console/core/namespace" >/dev/null || true

namespaces=$(curl --fail --silent --show-error "$nacos_base_url/v3/console/core/namespace/list")
printf '%s' "$namespaces" | grep -q "$namespace"
