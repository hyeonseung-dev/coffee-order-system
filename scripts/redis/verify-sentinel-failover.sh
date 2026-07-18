#!/usr/bin/env bash
set -euo pipefail
master_name=coffee-order-redis
sentinel=redis-sentinel-1
stopped_master=''
started_at=$(date '+%Y-%m-%dT%H:%M:%S')
log_file=$(mktemp)

cleanup() {
  [[ -n "$stopped_master" ]] && docker compose start "$stopped_master" >/dev/null || true
  rm -f "$log_file"
}
trap cleanup EXIT
sentinel_command() { docker compose exec -T "$sentinel" redis-cli -p 26379 "$@"; }
current_master() { sentinel_command SENTINEL GET-MASTER-ADDR-BY-NAME "$master_name" | head -1; }
opposite() { [[ "$1" == redis-master ]] && echo redis-replica || echo redis-master; }

stopped_master=$(current_master)
[[ "$stopped_master" == redis-master || "$stopped_master" == redis-replica ]] || { echo "Unknown master: $stopped_master" >&2; exit 1; }
new_master=$(opposite "$stopped_master")
sentinel_command SENTINEL CKQUORUM "$master_name"
echo "Stopping current master=$stopped_master at $started_at"
docker compose stop "$stopped_master"

for attempt in $(seq 1 20); do
  role=$(docker compose exec -T "$new_master" redis-cli INFO replication | awk -F: '/^role:/{print $2}' | tr -d '\r')
  reported=$(current_master)
  [[ "$role" == master && "$reported" == "$new_master" ]] && break
  sleep 2
done
[[ "$role" == master && "$reported" == "$new_master" ]] || { echo "Failover timeout: role=$role master=$reported" >&2; exit 1; }
docker compose logs --since "$started_at" --no-color redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 > "$log_file"
grep -Fq '+promoted-slave' "$log_file" || { echo 'Missing +promoted-slave log' >&2; exit 1; }
grep -Fq '+switch-master' "$log_file" || { echo 'Missing +switch-master log' >&2; exit 1; }

docker compose start "$stopped_master"
stopped_master=''
for attempt in $(seq 1 20); do
  info=$(docker compose exec -T "$(opposite "$new_master")" redis-cli INFO replication)
  role=$(printf '%s' "$info" | awk -F: '/^role:/{print $2}' | tr -d '\r')
  host=$(printf '%s' "$info" | awk -F: '/^master_host:/{print $2}' | tr -d '\r')
  link=$(printf '%s' "$info" | awk -F: '/^master_link_status:/{print $2}' | tr -d '\r')
  [[ "$role" == slave && "$host" == "$new_master" && "$link" == up ]] && exit 0
  sleep 2
done
echo "Replica rejoin timeout: role=$role host=$host link=$link" >&2
exit 1
