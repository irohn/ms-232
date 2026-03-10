#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [-u username] [-p password]" >&2
}

MS_USER=""
MS_PASS=""

while getopts ":u:p:h" opt; do
  case "$opt" in
    u)
      MS_USER="$OPTARG"
      ;;
    p)
      MS_PASS="$OPTARG"
      ;;
    h)
      usage
      exit 0
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      usage
      exit 1
      ;;
    \?)
      echo "Unknown option: -$OPTARG" >&2
      usage
      exit 1
      ;;
  esac
done

shift $((OPTIND - 1))

if [[ $# -gt 0 ]]; then
  echo "Unexpected argument: $1" >&2
  usage
  exit 1
fi

if [[ -z "$MS_USER" ]]; then
  read -rp "Username: " MS_USER
fi

if [[ -z "$MS_PASS" ]]; then
  read -rsp "Password: " MS_PASS
  echo
fi

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
