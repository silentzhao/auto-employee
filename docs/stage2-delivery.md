# Stage 2 Delivery Notes

## Services

- App: `employee-manager`, HTTP port `58080`.
- PostgreSQL: `postgres`, port `5432`.
- RabbitMQ: `rabbitmq`, port `5672`, management port `15672`.
- Local object storage path: `./var/object-storage`.

## Start

```powershell
docker compose up -d --build
```

RabbitMQ notification is controlled by `APP_ASYNC_RABBIT_ENABLED`. The application always writes async tasks to `async_tasks`, so a single-node trial can run with the built-in polling worker before enabling a dedicated RabbitMQ consumer.

## Operations

```powershell
docker compose logs -f employee-manager
.\scripts\db-backup.ps1
.\scripts\db-restore.ps1 -BackupFile .\var\backups\employee_manager-20260521-120000.dump
```

## Recovery

Failed async jobs are visible at `GET /api/admin/async-tasks/failures` and can be requeued with `POST /api/admin/async-tasks/{taskId}/retry`.

## Stage 2 API Additions

- `POST /api/admin/sessions/{sessionId}/takeover`
- `POST /api/admin/sessions/{sessionId}/resume-ai`
- `GET /api/admin/customers/{customerId}/tags`
- `GET /api/admin/follow-up-tasks`
- `PUT /api/admin/tenants/{tenantId}`
- `PUT /api/admin/tenants/{tenantId}/configs/{key}`
- `GET /api/admin/tenants/{tenantId}/configs`
