#!/usr/bin/env bash
set -euo pipefail

master_name=coffee-order-redis
sentinel=redis-sentinel-1

sentinel_command() {
  docker compose exec -T "$sentinel" redis-cli -p 26379 "$@"
}

echo '== quorum and initial topology =='
sentinel_command SENTINEL CKQUORUM "$master_name"
sentinel_command SENTINEL GET-MASTER-ADDR-BY-NAME "$master_name"
docker compose exec -T redis-master redis-cli INFO replication | grep -E 'role|connected_slaves'
docker compose exec -T redis-replica redis-cli INFO replication | grep -E 'role|master_link_status'

echo '== stop current master and wait for Sentinel promotion =='
docker compose stop redis-master
for attempt in $(seq 1 20); do
  role=$(docker compose exec -T redis-replica redis-cli INFO replication | grep '^role:' || true)
  if [[ "$role" == 'role:master' ]]; then
    break
  fi
  sleep 2
done

docker compose exec -T redis-replica redis-cli INFO replication | grep -E 'role|connected_slaves'
sentinel_command SENTINEL GET-MASTER-ADDR-BY-NAME "$master_name"
docker compose logs --no-color redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 \
  | grep -E '\+(sdown|odown|try-failover|selected-slave|promoted-slave|switch-master)'

echo '== restore old master and verify it rejoins as replica =='
docker compose start redis-master
for attempt in $(seq 1 20); do
  role=$(docker compose exec -T redis-master redis-cli INFO replication | grep '^role:' || true)
  if [[ "$role" == 'role:slave' ]]; then
    break
  fi
  sleep 2
done
docker compose exec -T redis-master redis-cli INFO replication | grep -E 'role|master_link_status'
