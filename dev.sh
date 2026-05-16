#!/usr/bin/env bash
#
# dev.sh — spin up two short-lived Cloudflare quick tunnels in front of the
# already-running docker-compose stack. While this script is alive:
#
#   https://<random>.trycloudflare.com         → frontend  (localhost:3000)
#   https://<random>.trycloudflare.com         → backend   (localhost:8081)
#
# Containers are restarted with tunnel-aware env (so CORS + the Nuxt public
# config point at the tunnel URLs). Ctrl-C tears down the tunnels and restarts
# the containers back to their local-only defaults.
#
# No persistent DNS, no auth — the tunnel URLs are unguessable but public. Stop
# the script when you're done.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

FRONT_LOG="$(mktemp -t chatapp-cf-front.XXXX.log)"
API_LOG="$(mktemp -t chatapp-cf-api.XXXX.log)"
FRONT_PID=""
API_PID=""
CLEANED=0

color()  { printf '\033[%sm%s\033[0m' "$1" "$2"; }
info()   { echo "$(color '36' '→')  $*"; }
ok()     { echo "$(color '32' '✓')  $*"; }
warn()   { echo "$(color '33' '!')  $*" >&2; }
die()    { echo "$(color '31' '✗')  $*" >&2; exit 1; }

cleanup() {
  [[ $CLEANED -eq 1 ]] && return 0
  CLEANED=1
  echo
  info "Stopping cloudflared..."
  for pid in "$FRONT_PID" "$API_PID"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  wait 2>/dev/null || true

  info "Reverting containers to localhost env..."
  docker compose up -d --no-deps backend frontend >/dev/null 2>&1 || \
    warn "Failed to revert containers — run 'docker compose up -d' manually."

  rm -f "$FRONT_LOG" "$API_LOG"
  ok "Tunnel torn down. Local stack is back to localhost-only."
}
trap cleanup EXIT INT TERM

# preflight
command -v cloudflared >/dev/null || die "cloudflared not found on PATH."
command -v docker      >/dev/null || die "docker not found on PATH."
docker compose version  >/dev/null 2>&1 || die "docker compose plugin not available."

# All three core services must already be up.
for svc in postgres backend frontend; do
  cid="$(docker compose ps -q "$svc" 2>/dev/null || true)"
  [[ -n "$cid" ]] || die "Service '$svc' is not running. Start it first: docker compose up -d"
done

# start tunnels
info "Starting Cloudflare quick tunnel for frontend (localhost:3000)..."
cloudflared --config /dev/null tunnel --url http://localhost:3000 --no-autoupdate \
  >"$FRONT_LOG" 2>&1 &
FRONT_PID=$!

info "Starting Cloudflare quick tunnel for backend  (localhost:8081)..."
cloudflared --config /dev/null tunnel --url http://localhost:8081 --no-autoupdate \
  >"$API_LOG" 2>&1 &
API_PID=$!

# Pull the public URL out of cloudflared's log. Times out at 60s.
wait_for_url() {
  local logfile="$1" label="$2" pid="$3"
  for _ in $(seq 1 60); do
    if ! kill -0 "$pid" 2>/dev/null; then
      cat "$logfile" >&2
      die "cloudflared for $label exited before publishing a URL."
    fi
    local url
    url="$(grep -oE 'https://[a-z0-9][a-z0-9-]*\.trycloudflare\.com' "$logfile" | head -1 || true)"
    if [[ -n "$url" ]]; then
      printf '%s' "$url"
      return 0
    fi
    sleep 1
  done
  cat "$logfile" >&2
  die "Timed out waiting for $label tunnel URL."
}

FRONT_PUBLIC_URL="$(wait_for_url "$FRONT_LOG" frontend "$FRONT_PID")"
API_PUBLIC_URL="$(wait_for_url "$API_LOG"   backend  "$API_PID")"
WS_PUBLIC_URL="wss://${API_PUBLIC_URL#https://}/ws"

export FRONT_PUBLIC_URL API_PUBLIC_URL WS_PUBLIC_URL

echo
# shellcheck disable=SC2005
echo "$(color '35;1')"
echo "  $(color '1' 'Frontend'):  $FRONT_PUBLIC_URL"
echo "  $(color '1' 'Backend ')$(color '1' ':')   $API_PUBLIC_URL"
echo "  $(color '1' 'WebSocket'): $WS_PUBLIC_URL"
# shellcheck disable=SC2005
echo "$(color '35;1')"
echo

#reload containers with tunnel env
info "Reloading backend + frontend with tunnel-aware env..."
docker compose \
  -f docker-compose.yml \
  -f docker-compose.tunnel.yml \
  up -d --no-deps backend frontend >/dev/null

ok "Tunnel is live. Press Ctrl-C to tear it down."
echo "    (backend boot takes ~10–30 s; the URL will 502 until Spring is ready)"
echo

# Block until either tunnel process dies.
wait -n "$FRONT_PID" "$API_PID" || true
