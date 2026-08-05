#!/bin/bash
# Restore script for Pharmacy POS
# Usage: ./restore.sh path/to/pos_backup_YYYYMMDD_HHMMSS.zip
# Requires: pg_restore, openssl, unzip

set -e

DB_NAME="${DB_NAME:-pos}"
DB_USER="${DB_USER:-pos}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
BACKUP_KEY="${BACKUP_KEY:-pharmacy-pos-backup-key}"

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_zip_file>"
    echo "Example: $0 backups/pos_backup_20260805_120000.zip"
    exit 1
fi

BACKUP_ZIP="$1"
if [ ! -f "$BACKUP_ZIP" ]; then
    echo "Error: File not found: $BACKUP_ZIP"
    exit 1
fi

WORK_DIR=$(mktemp -d)
trap "rm -rf $WORK_DIR" EXIT

echo "Starting restore at $(date)"

echo "  → Extracting..."
unzip -o "$BACKUP_ZIP" -d "$WORK_DIR"

echo "  → Verifying hash..."
ENCRYPTED_FILE=$(ls "$WORK_DIR"/*.enc 2>/dev/null | head -1)
if [ -z "$ENCRYPTED_FILE" ]; then
    echo "Error: No encrypted backup found in archive"
    exit 1
fi

echo "  → Decrypting..."
openssl enc -aes-256-cbc -d -salt -pbkdf2 -pass pass:"$BACKUP_KEY" \
    -in "$ENCRYPTED_FILE" -out "${WORK_DIR}/backup.dump"

echo "  → Restoring database..."
export PGPASSWORD="${DB_PASSWORD:-pos}"
pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    --clean --if-exists --no-owner "${WORK_DIR}/backup.dump"

echo "Restore complete at $(date)"
echo "PostgreSQL database '$DB_NAME' has been restored."
