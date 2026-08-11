# Pharmacy POS Pilot Installer

This installer is for partner testing and early pilots. It is not yet the
Docker-free commercial package.

## Client prerequisite

- Windows 10/11 x64
- Docker Desktop installed and started once
- Internet access during the first installation so Docker can download build
  dependencies and base images

## Build locally

Install Inno Setup 6, then run:

```powershell
.\installer\build-installer.ps1
```

The output is `outputs\installer\PharmacyPOS-Pilot-Setup.exe`.

## What setup does

1. Copies the complete API and frontend build contexts to the current user's
   local application directory.
2. Creates a unique PostgreSQL password in `.env.pilot`.
3. Builds and starts PostgreSQL, Spring Boot, and Next.js containers.
4. Waits for both health endpoints before completing.
5. Adds Open, Start, Stop, and Status shortcuts.

Reinstalling upgrades the application while retaining Docker data volumes.
Uninstalling stops the containers but intentionally retains the database
volumes for recovery.

The installation log is `pharmacy-pos-install.log` inside the installation
directory.
