#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FORECASTR="$ROOT/forecastr"
WEB="$FORECASTR/web"
SERVER_JAR="$FORECASTR/server/target/forecastr-server-1.0.0-SNAPSHOT.jar"
SERVER_LOG="/tmp/forecastr-web-server.log"
SERVER_PID=""
WEB_PID=""

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Fehler: '$1' nicht gefunden." >&2
    exit 1
  }
}

find_free_port() {
  python3 - "$1" "$2" <<'PY'
import socket
import sys

for port in range(int(sys.argv[1]), int(sys.argv[2]) + 1):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind(("127.0.0.1", port))
        except OSError:
            continue
        print(port)
        break
else:
    raise SystemExit("Kein freier Port im angegebenen Bereich.")
PY
}

wait_for_server() {
  local port="$1"
  local users_url="http://127.0.0.1:${port}/users"
  local feed_url="http://127.0.0.1:${port}/feed?limit=1"
  local attempt
  for attempt in $(seq 1 120); do
    if curl -sf "$users_url" >/dev/null 2>&1 && \
      curl -sf -H "X-Forecastr-User-Id: 1" "$feed_url" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "$SERVER_PID" ]] && ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "Fehler: Server ist vorzeitig beendet. Log: $SERVER_LOG" >&2
      tail -n 40 "$SERVER_LOG" >&2
      exit 1
    fi
    sleep 0.5
  done
  echo "Fehler: Server oder Feed sind nicht bereit. Log: $SERVER_LOG" >&2
  tail -n 40 "$SERVER_LOG" >&2
  exit 1
}

wait_for_web() {
  local port="$1"
  local url="http://127.0.0.1:${port}/"
  local attempt
  for attempt in $(seq 1 120); do
    if curl -sf "$url" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "$WEB_PID" ]] && ! kill -0 "$WEB_PID" 2>/dev/null; then
      echo "Fehler: Webclient ist vorzeitig beendet." >&2
      exit 1
    fi
    if [[ -n "$SERVER_PID" ]] && ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "Fehler: Server ist vorzeitig beendet. Log: $SERVER_LOG" >&2
      tail -n 40 "$SERVER_LOG" >&2
      exit 1
    fi
    sleep 0.25
  done
  echo "Fehler: Webclient antwortet nicht auf $url." >&2
  exit 1
}

cleanup() {
  if [[ -n "$WEB_PID" ]] && kill -0 "$WEB_PID" 2>/dev/null; then
    kill "$WEB_PID" 2>/dev/null || true
    wait "$WEB_PID" 2>/dev/null || true
  fi
  if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}

require java
require mvn
require node
require npm
require python3
require curl

trap cleanup EXIT INT TERM

echo "Baue den Forecastr-Server..."
(cd "$FORECASTR" && mvn -DskipTests package)

if [[ ! -d "$WEB/node_modules" ]]; then
  echo "Installiere Web-Abhängigkeiten..."
  (cd "$WEB" && npm ci)
fi

SERVER_PORT="$(find_free_port 8080 8180)"
WEB_PORT="$(find_free_port 5173 5273)"
echo "Starte Server auf Port $SERVER_PORT (Log: $SERVER_LOG)..."
java -jar "$SERVER_JAR" --server.port="$SERVER_PORT" --spring.profiles.active=test >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!

wait_for_server "$SERVER_PORT"
echo "Starte Webclient auf Port $WEB_PORT..."
(cd "$WEB" && FORECASTR_PROXY_TARGET="http://127.0.0.1:$SERVER_PORT" \
  npm run dev -- --host 127.0.0.1 --port "$WEB_PORT" --strictPort) &
WEB_PID=$!

wait_for_web "$WEB_PORT"
echo "Webclient bereit unter http://127.0.0.1:$WEB_PORT"
echo "Hinweis: Dieses Terminal geöffnet lassen. Strg+C beendet Webclient und Server."
wait "$WEB_PID"
