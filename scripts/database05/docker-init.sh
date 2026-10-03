#!/bin/sh
set -eu
cd /opt/palco/database05
psql -X --no-password -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f init.sql
