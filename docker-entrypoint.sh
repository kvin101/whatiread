#!/bin/sh
set -e

avatar_dir="${WHATIREAD_AVATARS_DIRECTORY:-/data/avatars}"
mkdir -p "$avatar_dir" 2>/dev/null || true

exec java -jar app.jar "$@"
