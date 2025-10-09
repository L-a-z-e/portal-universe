#!/usr/bin/env bash
set -euo pipefail

# Usage:
# docker-entrypoint-wait.sh <host> <port> [<path>] [<timeout_seconds>] -- <command...>
# Examples:
# docker-entrypoint-wait.sh discovery-service 8761 /actuator/health 120 -- java -jar /app.jar
# docker-entrypoint-wait.sh config-service 8888 /actuator/health 60 -- java -jar /app.jar

host="$1"
port="$2"
# optional: path to check (default /actuator/health)
path="${3:-/actuator/health}"
# optional: timeout in seconds (default 30)
timeout="${4:-30}"

# If command arguments are provided after '--', we capture them. Otherwise we run nothing.
# Find '--' separator (if present) and shift past the first 4 args + separator.
shift_count=2
if [ "${3-}" != "" ]; then shift_count=$((shift_count + 1)); fi
if [ "${4-}" != "" ]; then shift_count=$((shift_count + 1)); fi

# If there is a '--' token we need to shift to the command args after it.
# Find '--' among the remaining args:
cmd=()
for i in "$@"; do
  if [ "$i" = "--" ]; then
    # shift past all args up to '--'
    while [ "$#" -gt 0 ]; do
      if [ "$1" = "--" ]; then
        shift
        break
      fi
      shift
    done
    # remaining args are the command
    cmd=( "$@" )
    break
  fi
done

# If no '--' given, treat all remaining args as the command
if [ ${#cmd[@]} -eq 0 ] && [ "$#" -gt 0 ]; then
  cmd=( "$@" )
fi

echo "Waiting for http://$host:$port$path  (timeout: ${timeout}s)..."

# Curl options used:
# -s : silent (no progress meter)
# -S : show error (in combination with -s)
# -f : fail silently on HTTP errors (exit code != 0 for 4xx/5xx)
# --max-time N : maximum time in seconds for the whole operation
curl_opts=( -sS -f --max-time 10 )

start_ts=$(date +%s)
end_ts=$((start_ts + timeout))

while true; do
  # Perform the HTTP request and check exit code
  if curl "${curl_opts[@]}" "http://$host:$port$path" >/dev/null 2>&1; then
    echo "OK: http://$host:$port$path is responding."
    # Exec the command to replace the shell (propagate signals properly)
    if [ ${#cmd[@]} -gt 0 ]; then
      exec "${cmd[@]}"
    else
      exit 0
    fi
  fi

  now_ts=$(date +%s)
  if [ "$now_ts" -ge "$end_ts" ]; then
    echo "Error: Timeout after ${timeout}s waiting for http://$host:$port$path"
    exit 1
  fi

  # Print a status message every 5 seconds
  sleep 1
done
