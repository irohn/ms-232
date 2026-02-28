#!/usr/bin/env bash
set -euo pipefail

read -rp "Username: " MS_USER
read -rsp "Password: " MS_PASS
echo

MS_TOKEN="$(
  curl -fsS -X POST "http://localhost:3000/api/login" \
    -H "Content-Type: application/json" \
    --data "$(printf '{"username":"%s","password":"%s"}' "$MS_USER" "$MS_PASS")" \
  | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p'
)"

if [[ -z "${MS_TOKEN:-}" ]]; then
  echo "Failed to get login token from http://localhost:3000/api/login"
  exit 1
fi

GAME_DIR="/mnt/c/Program Files (x86)/Steam/steamapps/content/app_216150/depot_216151"
EXE_NAME="MapleStory.exe"

cd "$GAME_DIR"
cmd.exe /C start "" "$EXE_NAME" WebStart "$MS_TOKEN"
