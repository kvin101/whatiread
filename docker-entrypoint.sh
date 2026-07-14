#!/bin/sh
set -e

avatar_dir="${WHATIREAD_AVATARS_DIRECTORY:-/data/avatars}"
mkdir -p "$avatar_dir"

if [ "$(id -u)" = "0" ]; then
  chown -R whatiread:whatiread "$avatar_dir"
  exec su-exec whatiread:whatiread java -jar app.jar "$@"
fi

exec java -jar app.jar "$@"
