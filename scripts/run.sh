#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FORECASTR="$ROOT/forecastr"
SERVER_JAR="$FORECASTR/server/target/forecastr-server-1.0.0-SNAPSHOT.jar"
CLIENT_JAR="$FORECASTR/client/target/client.jar"
SERVER_LOG="/tmp/forecastr-server.log"
SERVER_PID=""

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Fehler: '$1' nicht gefunden." >&2
    exit 1
  }
}

find_free_port() {
  python3 - <<'PY'
import socket

for port in range(8080, 8181):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind(("127.0.0.1", port))
        except OSError:
            continue
        print(port)
        break
else:
    raise SystemExit("Kein freier Port zwischen 8080 und 8180.")
PY
}

wait_for_server() {
  local port="$1"
  local url="http://127.0.0.1:${port}/users"
  local i
  for i in $(seq 1 120); do
    if curl -sf "$url" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "$SERVER_PID" ]] && ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "Fehler: Server ist vorzeitig beendet. Log: $SERVER_LOG" >&2
      tail -n 40 "$SERVER_LOG" >&2
      exit 1
    fi
    sleep 0.5
  done
  echo "Fehler: Server antwortet nicht auf $url. Log: $SERVER_LOG" >&2
  tail -n 40 "$SERVER_LOG" >&2
  exit 1
}

cleanup() {
  if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}

require java
require mvn
require python3
require curl

trap cleanup EXIT INT TERM

echo "Baue Forecastr..."
(cd "$FORECASTR" && mvn clean package)

PORT="$(find_free_port)"
echo "Starte Server auf Port $PORT (Log: $SERVER_LOG)..."
java -jar "$SERVER_JAR" --server.port="$PORT" --spring.profiles.active=test >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!

wait_for_server "$PORT"
echo "Server bereit unter http://localhost:$PORT"

java -jar "$CLIENT_JAR" --server "http://localhost:$PORT" "$@"
