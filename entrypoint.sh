#!/bin/sh
# Fix permissions on mounted volumes (runs as root initially)
chown -R pos:pos /app/backups /app/pos-data /connectors 2>/dev/null || true
exec su-exec pos java -jar app.jar
