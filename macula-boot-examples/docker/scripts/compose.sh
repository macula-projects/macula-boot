#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
docker_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
env_file=${MACULA_COMPOSE_ENV:-$docker_dir/.env}

if [ ! -f "$env_file" ]; then
  env_file=$docker_dir/.env.example
fi

compose() {
  docker compose --env-file "$env_file" -f "$docker_dir/docker-compose.yml" "$@"
}

profiles() {
  case "$1" in
    alibaba) printf '%s\n' '--profile' 'alibaba' ;;
    tencent) printf '%s\n' '--profile' 'tencent' ;;
    all) printf '%s\n' '--profile' 'alibaba' '--profile' 'tencent' ;;
    *)
      echo "Unknown stack: $1 (expected alibaba, tencent, or all)" >&2
      exit 2
      ;;
  esac
}

middleware() {
  case "$1" in
    alibaba) printf '%s\n' 'mysql' 'redis' 'nacos-init' ;;
    tencent) printf '%s\n' 'mysql' 'redis' 'polaris' ;;
    all) printf '%s\n' 'mysql' 'redis' 'nacos-init' 'polaris' ;;
  esac
}

command=${1:-help}
if [ "$#" -gt 0 ]; then
  shift
fi
stack=all
case "${1:-}" in
  alibaba|tencent|all)
    stack=$1
    shift
    ;;
esac

case "$command" in
  up)
    compose up -d $(middleware "$stack") "$@"
    ;;
  up-apps)
    compose $(profiles "$stack") up -d --build --wait "$@"
    ;;
  build)
    compose $(profiles "$stack") build "$@"
    ;;
  status)
    compose $(profiles "$stack") ps -a
    ;;
  logs)
    compose $(profiles "$stack") logs -f "$@"
    ;;
  down)
    compose --profile alibaba --profile tencent down --remove-orphans
    ;;
  reset)
    if [ "${1:-}" != "--confirm" ]; then
      echo "This deletes all local Macula Boot example data." >&2
      echo "Re-run: $0 reset $stack --confirm" >&2
      exit 2
    fi
    compose --profile alibaba --profile tencent down --volumes --remove-orphans
    ;;
  config)
    compose $(profiles "$stack") config
    ;;
  *)
    echo "Usage: $0 {up|up-apps|build|status|logs|down|reset|config} {alibaba|tencent|all} [--confirm|service ...]" >&2
    exit 2
    ;;
esac
