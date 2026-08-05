#!/bin/bash
# Encrypted local backup script for Pharmacy POS
# Usage: ./backup.sh [output_dir]
# Requires: pg_dump (included with PostgreSQL), openssl, zip

set -e

DB_NAME="${DB_NAME:-pos}"
DB_USER="${DB_USER:-pos}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
BACKUP_DIR="${1:-./backups}"
BACKUP_KEY="${BACKUP_KEY:-pharmacy-pos-backup-key}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/pos_backup_${TIMESTAMP}"

mkdir -p "$BACKUP_DIR"

echo "Starting backup at $(date)"
export PGPASSWORD="${DB_PASSWORD:-pos}"

echo "  → Dumping database..."
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F c -f "${BACKUP_FILE}.dump"

echo "  → Encrypting..."
openssl enc -aes-256-cbc -salt -pbkdf2 -pass pass:"$BACKUP_KEY" \
    -in "${BACKUP_FILE}.dump" -out "${BACKUP_FILE}.enc"

echo "  → Verifying hash..."
sha256sum "${BACKUP_FILE}.dump" > "${BACKUP_FILE}.sha256"

rm "${BACKUP_FILE}.dump"

echo "  → Compressing..."
zip -j "${BACKUP_FILE}.zip" "${BACKUP_FILE}.enc" "${BACKUP_FILE}.sha256"
rm "${BACKUP_FILE}.enc" "${BACKUP_FILE}.sha256"

echo "Backup complete: ${BACKUP_FILE}.zip"
echo "Size: $(du -h "${BACKUP_FILE}.zip" | cut -f1)"

# Keep last 30 days of backups
ls -t "$BACKUP_DIR"/pos_backup_*.zip 2>/dev/null | tail -n +31 | xargs rm -f 2>/dev/null || true
echo "Old backups cleaned (keeping 30 days)"
