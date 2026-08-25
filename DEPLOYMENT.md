# Deployment & Operations Runbook

Pilot deployment = Docker Compose (`docker-compose.pilot.yml`) driving three
containers: postgres, api, frontend. All commands below run from the repo root
with `.env.pilot` present.

## First install (fresh site)

1. Copy the repo to the shop machine; install Docker Desktop.
2. Create `.env.pilot`:
   ```
   POSTGRES_PASSWORD=<long random>
   POS_SEED_DEMO_ENABLED=false
   SHOW_DEMO_ACCOUNTS=false
   FRONTEND_PORT=3000
   API_PORT=9090
   POS_PHARMACY_NAME=<Shop name>
   POS_BOOTSTRAP_ADMIN_EMAIL=<owner email>
   POS_BOOTSTRAP_ADMIN_PASSWORD=<min 8 chars>   # omit to auto-generate (printed once in API logs)
   ```
3. `docker compose -f docker-compose.pilot.yml --env-file .env.pilot up -d`
4. First API boot prints the generated owner password once if you omitted it:
   `docker logs pharmacy-pos-pilot-api-1 | Select-String BOOTSTRAP`
5. Sign in at `http://localhost:3000`, change the password, create staff.

## Nightly backup

```
powershell -File scripts\backup-db.ps1              # keeps 14 days
powershell -File scripts\backup-db.ps1 -KeepDays 30 # longer retention
```

Schedule it (Task Scheduler, daily 23:30):
```
schtasks /create /tn "PharmacyBackup" /sc daily /st 23:30 /tr ^
  "powershell -ExecutionPolicy Bypass -File C:\pos\scripts\backup-db.ps1"
```
Copy `backups\*.dump` off-site weekly (USB/OneDrive).

## Restore

```
powershell -File scripts\restore-db.ps1 -File backups\pharmacy_pos_<stamp>.dump
```
The script first validates the dump against a scratch database (live data
untouched on failure), stops the API, replaces the database, and restarts.
Type `RESTORE` to confirm.

## Upgrading to a new version

```
powershell -File scripts\backup-db.ps1                 # 1. snapshot first
git pull                                               # 2. new code
docker compose -f docker-compose.pilot.yml --env-file .env.pilot up -d --build
```
Flyway applies pending migrations on API start. Verify:
`docker logs pharmacy-pos-pilot-api-1 | Select-String "Successfully validated"`.

### Rollback

1. `git checkout <previous-tag>`
2. `docker compose ... up -d --build`
3. If migrations must be reversed: restore the pre-upgrade backup (step above).

Keep at least the last two images/tags before upgrading.

## Secrets rotation

| Secret | Where | Rotation |
|--------|-------|----------|
| Daraja consumer secret / passkey | System page → M-Pesa panel | Save new values; blank field keeps old |
| Postgres password | `.env.pilot` + volume | Change in compose + `ALTER USER`; recreate containers |
| Session HMAC n/a | — | Sessions are DB-backed; logout revokes |

Daraja credentials never leave the server except masked (`********`) in API
responses. M-Pesa gateway callbacks are disabled until signature verification
is enabled — STK completes via polling instead.

## Health & diagnostics

- Stack status: `docker compose -f docker-compose.pilot.yml ps`
- API health: `http://localhost:9090/actuator/health`
- Frontend: `http://localhost:3000/login` (HTTP 200)
- Request correlation: every response carries `X-Request-ID`; grep API logs
  with it to trace one request end-to-end.
- Backups land in `<repo>\backups\`.
