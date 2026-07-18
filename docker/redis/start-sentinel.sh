#!/bin/sh
set -eu

attempt=1
max_attempts=30

while [ "$attempt" -le "$max_attempts" ]; do
  if getent hosts redis-master >/dev/null 2>&1 \
    && redis-cli -h redis-master -p 6379 ping 2>/dev/null | grep -qx PONG; then
    exec redis-server /tmp/sentinel.conf --sentinel
  fi

  echo "Waiting for redis-master DNS and PING (${attempt}/${max_attempts})" >&2
  attempt=$((attempt + 1))
  sleep 1
done

echo "redis-master was not ready before Sentinel startup timeout" >&2
exit 1
