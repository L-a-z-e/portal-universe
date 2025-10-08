#!/bin/bash
# docker-entrypoint-wait.sh

host="$1"
port="$2"
timeout="${3:-30}" # Default timeout to 30 seconds
shift 3
cmd="$@"

>&2 echo "Waiting for $host:$port to be available..."

for i in $(seq 1 $timeout); do
  if nc -z "$host" "$port"; then
    >&2 echo "$host:$port is up - executing command"
    exec $cmd
  fi
  if [ $((i % 5)) -eq 0 ]; then # Log every 5 seconds
    >&2 echo "$host:$port is still unavailable - waiting ($i/$timeout)"
  fi
  sleep 1
done

>&2 echo "Error: $host:$port did not become available within $timeout seconds."
exit 1