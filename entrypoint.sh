#!/bin/sh
# Fix permissions on mounted volumes (runs as root initially)
chown -R pos:pos /app/backups /app/pos-data /connectors 2>/dev/null || true

# Wait for PostgreSQL to be ready (extracted from JDBC URL)
DB_HOST="${SPRING_DATASOURCE_URL##*//}"
DB_HOST="${DB_HOST%%:*}"
DB_PORT="${SPRING_DATASOURCE_URL##*:}"
DB_PORT="${DB_PORT%%/*}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-pharmacy_pos}"

echo "Waiting for PostgreSQL at $DB_HOST:$DB_PORT..."
retries=0
max_retries=30
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -q 2>/dev/null; do
  retries=$((retries + 1))
  if [ "$retries" -ge "$max_retries" ]; then
    echo "PostgreSQL not ready after $max_retries attempts, starting anyway (HikariCP will retry)..."
    break
  fi
  echo "  PostgreSQL not ready (attempt $retries/$max_retries), retrying in 2s..."
  sleep 2
done

if [ "$retries" -lt "$max_retries" ]; then
  echo "PostgreSQL is ready."
fi

exec su-exec pos java -jar app.jar
