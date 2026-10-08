# Deployment preparation checklist

This document describes configuration requirements only. No deployment is performed by this repository workflow.

## Build and runtime

- Build and validate with `./mvnw verify` (Windows: `mvnw.cmd verify`).
- Build the container image with `docker build -t gerenciamentissimoflow:local .`.
- Run behind a TLS-terminating platform/load balancer; expose only the application HTTP port.
- Use a managed PostgreSQL instance or an equivalently backed-up PostgreSQL service. Compose credentials are local examples, not production secrets.

## Required environment

Set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 random bytes), `JWT_EXPIRATION_SECONDS`, and `CORS_ALLOWED_ORIGINS` to explicit production values. The secret is mandatory and validated at startup; retain the independent security review gate before public deployment. Do not enable demo seeding or add demo credentials to production.

## Schema, health, and rollback

Flyway applies versioned migrations at startup; rehearse migration compatibility and backup/restore against a non-production copy before rollout. Health details are not exposed; use `/actuator/health` for platform health checks. Before schema changes, take a database backup and prepare a forward-fix/restore plan—do not assume application rollback can reverse a committed migration.

## Security and operations

- Restrict CORS origins to exact trusted origins; never use wildcard origins with credentials.
- Keep Actuator limited to health/info and do not expose management ports publicly.
- Supply database credentials through the platform secret store; never commit `.env` or secret values.
- Keep request logs free of passwords, tokens, and sensitive payloads; monitor application and database health.
- Verify role enforcement, audit access, backups, retention, and recovery before any public launch.

## Readiness limitation

Authentication endpoints and bearer-token validation exist, but public deployment still requires independent security review and is not part of local validation. Deployment must be separately authorized.
