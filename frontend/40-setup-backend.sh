#!/bin/sh
set -e

if [ -z "$BACKEND_URL" ]; then
    if getent hosts api >/dev/null 2>&1; then
        BACKEND_URL="http://api:8080"
    else
        BACKEND_URL="http://host.docker.internal:8080"
    fi
fi

BACKEND_URL=$(echo "$BACKEND_URL" | sed 's|/*$||')

RESOLVERS=$(awk 'BEGIN{ORS=" "} $1=="nameserver" {print $2}' /etc/resolv.conf 2>/dev/null)
[ -z "$RESOLVERS" ] && RESOLVERS="127.0.0.11 8.8.8.8"

sed -e "s|__RESOLVER__|${RESOLVERS}|g" -e "s|__BACKEND_URL__|${BACKEND_URL}|g" /etc/nginx/default.conf.template > /etc/nginx/conf.d/default.conf
