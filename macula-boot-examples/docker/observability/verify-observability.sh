#!/usr/bin/env sh
set -eu

REQUEST_URL="${1:-http://127.0.0.1:5000/consumer/api/v1/consumer/echo/demo?str=hello}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://127.0.0.1:9090}"
LOKI_URL="${LOKI_URL:-http://127.0.0.1:3100}"
TEMPO_URL="${TEMPO_URL:-http://127.0.0.1:3200}"
HEADER_FILE="${TMPDIR:-/tmp}/macula-observability-headers.$$"
BODY_FILE="${TMPDIR:-/tmp}/macula-observability-body.$$"
trap 'rm -f "$HEADER_FILE" "$BODY_FILE"' EXIT

curl --fail --silent --show-error --dump-header "$HEADER_FILE" --output "$BODY_FILE" "$REQUEST_URL"
TRACE_ID=$(awk 'BEGIN { IGNORECASE=1 } /^x-traceId:/ { gsub("\r", "", $2); print $2 }' "$HEADER_FILE" | tail -1)
if [ -z "$TRACE_ID" ]; then
  echo "No x-traceId response header returned by $REQUEST_URL" >&2
  exit 1
fi

MAX_ATTEMPTS="${OBSERVABILITY_QUERY_MAX_ATTEMPTS:-30}"
RETRY_SECONDS="${OBSERVABILITY_QUERY_RETRY_SECONDS:-2}"

wait_for() {
  description="$1"
  shift
  attempt=1
  while ! "$@"; do
    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
      echo "Timed out waiting for $description after $MAX_ATTEMPTS attempts" >&2
      return 1
    fi
    attempt=$((attempt + 1))
    sleep "$RETRY_SECONDS"
  done
}

tempo_has_distributed_trace() {
  curl --fail --silent --show-error "$TEMPO_URL/api/traces/$TRACE_ID" \
    | jq -e '
        [.batches[].resource.attributes[]
          | select(.key == "service.name")
          | .value.stringValue] as $services
        | (["macula-example-alibaba-gateway",
            "macula-example-alibaba-consumer",
            "macula-example-alibaba-provider1"]
          | all(. as $service | $services | index($service)))
      ' >/dev/null
}

loki_has_log() {
  message="$1"
  curl --get --fail --silent --show-error "$LOKI_URL/loki/api/v1/query_range" \
    --data-urlencode "query={service_name=\"macula-example-alibaba-consumer\"} | trace_id=\"$TRACE_ID\" |= \"$message\"" \
    | grep -q '"result":\[{'
}

prometheus_has_metric() {
  curl --get --fail --silent --show-error "$PROMETHEUS_URL/api/v1/query" \
    --data-urlencode 'query={__name__=~".+",service_name="macula-example-alibaba-consumer"}' \
    | grep -q '"result":\[{'
}

wait_for "Gateway/Consumer/Provider trace $TRACE_ID in Tempo" tempo_has_distributed_trace
wait_for "synchronous log for $TRACE_ID in Loki" loki_has_log "consumer echo by demo"
wait_for "managed-async log for $TRACE_ID in Loki" loki_has_log "consumer managed-async echo by demo"
wait_for "consumer metric in Prometheus" prometheus_has_metric

echo "Verified trace, synchronous log, managed-async log and metric for $TRACE_ID"
