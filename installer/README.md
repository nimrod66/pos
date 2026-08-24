# Pharmacy POS Pilot Installer

This installer is for partner testing and early pilots. It is not yet the
Docker-free commercial package.

## Client prerequisite

- Windows 10/11 x64
- Docker Desktop installed and started once
- Internet access during the first installation so Docker can download build
  dependencies and base images

## Build locally

Install Python 3.13 and Inno Setup 6, then run:

```powershell
python -m pip install -r connectors\requirements.txt "PyInstaller>=6.0,<7"
.\installer\build-hardware-connector.ps1
.\installer\build-installer.ps1
```

The output is `outputs\installer\PharmacyPOS-Pilot-Setup.exe`.

## What setup does

1. Copies the complete API and frontend build contexts to the current user's
   local application directory.
2. Creates a unique PostgreSQL password in `.env.pilot`.
3. Builds and starts PostgreSQL, Spring Boot, and Next.js containers.
4. Starts the bundled local hardware connector without requiring Python on the client.
5. Waits for the application and connector health endpoints before completing.
6. Adds Open, Start, Stop, Status, encrypted Backup, guarded Restore, and
   verified Update shortcuts.

Reinstalling upgrades the application while retaining Docker data volumes.
Uninstalling stops the containers but intentionally retains the database
volumes for recovery.

The installation log is `pharmacy-pos-install.log` inside the installation
directory.

Printer, scanner, cash-drawer, and customer-display ports are configured from
the terminal Hardware setup screen. The local profile is stored as
`connectors\hardware_config.json` in the installation directory. The connector
binds only to `127.0.0.1:9100`.

## Backup and restore

Use the Start menu shortcuts installed with Pharmacy POS. Backups are written
to `Documents\Pharmacy POS Backups` as authenticated, passphrase-encrypted
`.pposbackup` files. The passphrase is not stored by the application.

Restore requires the backup passphrase and an explicit `RESTORE` confirmation.
It creates a fresh safety backup, stops the API and frontend, replaces the
PostgreSQL database, and starts the application again. Keep at least one tested
backup on a separate encrypted drive.

## Updates

The update shortcut reads the version manifest from `Mark-Gachau/pos`, then
downloads the matching pilot release installer and its separately published
SHA-256 checksum. It verifies the file and asks for an explicit `INSTALL`
confirmation before opening it. Updates keep the existing Docker database
volume. Make and retain a backup before updating.

Pilot installers are not yet code-signed. Code signing remains a production
release requirement even though checksum verification protects against a
damaged or mismatched download.
