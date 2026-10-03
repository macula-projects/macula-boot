#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
deploy_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
env_file=${MACULA_COMPOSE_ENV:-$deploy_dir/.env}

if [ ! -f "$env_file" ]; then
  env_file=$deploy_dir/.env.example
fi

compose() {
  docker compose --env-file "$env_file" -f "$deploy_dir/docker-compose.yml" "$@"
}

command=${1:-help}
case "$command" in
  up)
    shift
    compose up -d --wait "$@"
    ;;
  up-apps)
    shift
    compose --profile apps up -d --build --wait "$@"
    ;;
  build)
    shift
    compose --profile apps build "$@"
    ;;
  status)
    compose --profile apps ps -a
    ;;
  logs)
    shift
    compose --profile apps logs -f "$@"
    ;;
  down)
    compose --profile apps down --remove-orphans
    ;;
  reset)
    if [ "${2:-}" != "--confirm" ]; then
      echo "This deletes all local generated-project data." >&2
      echo "Re-run: $0 reset --confirm" >&2
      exit 2
    fi
    compose --profile apps down --volumes --remove-orphans
    ;;
  config)
    compose config
    ;;
  config-apps)
    compose --profile apps config
    ;;
  *)
    echo "Usage: $0 {up [service ...]|up-apps [service ...]|build [service ...]|status|logs [service ...]|down|reset --confirm|config|config-apps}" >&2
    exit 2
    ;;
esac
