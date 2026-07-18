#!/usr/bin/env bash
set -euo pipefail

scenario=${1:-}
base_url=${BASE_URL:-http://host.docker.internal:8080}
project_root=$(cd "$(dirname "$0")/../.." && pwd)
k6_script_dir="$project_root/scripts/k6"
business_date=$(TZ=Asia/Seoul date +%F)
cache_key="popular:menus:7days:${business_date}:v1"

case "$scenario" in
  mysql)
    script=scripts/k6/popular-menu-mysql-baseline.js
    ;;
  hit)
    script=scripts/k6/popular-menu-cache-hit.js
    ;;
  miss)
    script=scripts/k6/popular-menu-cache-miss.js
    ;;
  *)
    echo "Usage: $0 {mysql|hit|miss}" >&2
    exit 64
    ;;
esac

run_k6() {
  docker run --rm -e BASE_URL="$base_url" -v "$k6_script_dir:/scripts:ro" grafana/k6:latest run "/scripts/${script##*/}"
}

wait_between_runs() {
  if [[ "$run" -lt 3 ]]; then
    sleep 10
  fi
}

if [[ "$scenario" == "mysql" ]]; then
  echo "MySQL baseline: start Spring Boot with POPULAR_MENU_CACHE_ENABLED=false before running this command."
  for run in 1 2 3; do
    echo "== MySQL baseline run $run/3 =="
    run_k6
    wait_between_runs
  done
elif [[ "$scenario" == "hit" ]]; then
  for run in 1 2 3; do
    echo "== Cache Hit run $run/3 =="
    curl --fail --silent --show-error "$base_url/api/menus/popular" >/dev/null
    run_k6
    wait_between_runs
  done
else
  for run in 1 2 3; do
    echo "== Cache Miss cold request $run/3 =="
    master=$(docker compose -f "$project_root/docker-compose.yml" exec -T redis-sentinel-1 redis-cli -p 26379 \
      SENTINEL GET-MASTER-ADDR-BY-NAME coffee-order-redis | head -1)
    case "$master" in redis-master|redis-replica) ;; *) echo "Cannot map Sentinel master: $master" >&2; exit 1;; esac
    deleted=$(docker compose -f "$project_root/docker-compose.yml" exec -T "$master" redis-cli DEL "$cache_key")
    [[ "$deleted" == 0 || "$deleted" == 1 ]] || { echo "DEL failed: $deleted" >&2; exit 1; }
    exists=$(docker compose -f "$project_root/docker-compose.yml" exec -T "$master" redis-cli EXISTS "$cache_key")
    [[ "$exists" == 0 ]] || { echo "Cache key still exists: $exists" >&2; exit 1; }
    echo "Cache key deleted from current master=$master"
    run_k6
    wait_between_runs
  done
fi
