# Azure Package Registry Setup Plan

This document outlines what is needed to build and operate the Azure-hosted package registry for LuaMade.

This Azure service is intended to live in a separate repository from the mod code.

## Goals

- Provide a trusted package source for LuaMade package commands.
- Allow package installs even when generic web fetch commands are disabled in-game.
- Support publishing community libraries and programs with moderation controls.
- Keep costs predictable and operations simple for early releases.

## Scope

### In scope

- Registry API for search, metadata, and artifact download.
- Publisher authentication and package upload.
- Package validation (manifest checks, file/path safety, checksum generation).
- Immutable versioning and package metadata storage.
- Basic moderation (unlist, revoke, disable publisher token).

### Out of scope (initially)

- Complex social features (ratings/comments).
- Automatic malware analysis beyond structural validation.
- Multi-region active-active failover.

## Proposed Azure Architecture

- Azure App Service (or Azure Container Apps): API host.
- Azure Blob Storage: package artifacts.
- Azure Database for PostgreSQL (or Cosmos DB): metadata, users/tokens, audit logs.
- Azure Key Vault: signing keys and sensitive secrets.
- Azure Front Door or Azure CDN: cached artifact delivery.
- Azure Monitor + Log Analytics: observability.

## Repository Layout (separate Azure repo)

- `api/` REST API implementation.
- `worker/` background jobs (optional: indexing, cleanup).
- `infra/` IaC (Bicep or Terraform).
- `docs/` API contract, runbooks, incident playbooks.
- `.github/workflows/` CI/CD pipelines.

## API Contract (minimum)

### Public read endpoints

- `GET /v1/search?q=<query>`
- `GET /v1/packages/<name>`
- `GET /v1/packages/<name>/<version>`
- `GET /v1/packages/<name>/<version>/download`

### Authenticated write endpoints

- `POST /v1/publish` (new package/version upload)
- `POST /v1/packages/<name>/<version>/unlist`
- `POST /v1/packages/<name>/<version>/revoke`

### Health endpoints

- `GET /health/live`
- `GET /health/ready`

## Package Format and Validation

### Required manifest fields

- `name`
- `version` (semver)
- `type` (`library` or `program`)
- `description`
- `entrypoints`

### Server-generated metadata

- `sha256`
- `artifactSizeBytes`
- `publishedAt`
- `publisherId`

### Validation checks

- Enforce package name/version rules.
- Reject duplicate version publish.
- Reject unsafe archive entries (`..`, absolute paths, drive letters).
- Enforce max archive size and max extracted file count.
- Require UTF-8 text for Lua sources when expected.

## Security Model

- TLS only.
- Publisher token auth (start simple with scoped API tokens).
- Token hashing at rest.
- Optional artifact signing (recommended by phase 2).
- Per-IP and per-token rate limiting.
- Audit logging for all write actions.
- Manual revoke/unlist controls.

## Data Model (minimum)

### packages

- `id`, `name`, `latestVersion`, `createdAt`, `status`

### package_versions

- `id`, `packageId`, `version`, `type`, `description`, `manifestJson`
- `sha256`, `artifactUrl`, `artifactSizeBytes`, `publishedAt`, `status`

### publishers

- `id`, `displayName`, `createdAt`, `status`

### api_tokens

- `id`, `publisherId`, `tokenHash`, `scopes`, `createdAt`, `expiresAt`, `status`

### audit_events

- `id`, `actorId`, `action`, `targetType`, `targetId`, `metadataJson`, `createdAt`

## CI/CD and Environments

### Environments

- `dev`: rapid iteration, lower limits.
- `staging`: production-like validation.
- `prod`: locked deployment approvals.

### Pipeline requirements

- Build + test on pull requests.
- Security scan (SAST + dependency scan).
- IaC validation/plan on infra changes.
- Artifact publish and deployment with approvals for staging/prod.
- Rollback procedure for failed deploys.

## Observability and Operations

- Structured logs with request IDs.
- Metrics:
  - request rate, error rate, latency
  - upload/download bytes
  - package publish success/failure
- Alerts:
  - elevated 5xx rate
  - storage/API throttling
  - auth failures spike
- Runbooks:
  - revoke compromised token
  - unlist/revoke package version
  - rotate secrets and signing keys

## Cost Controls

- Blob lifecycle policies for old artifacts/logs.
- CDN caching for package downloads.
- API response caching for search/metadata where safe.
- Quotas for publish size/rate per publisher.

## Rollout Plan

1. Phase 1: Read-only registry + static metadata + downloads.
2. Phase 2: Authenticated publish flow + validation + moderation.
3. Phase 3: Optional signing, advanced search, and publisher management UX.

## Integration Notes for LuaMade

- Set package manager base URL in:
  - `config/luamade/package_manager_base_url.txt`
- Keep trusted domain controls aligned with:
  - `config/luamade/trusted_domains.txt`
- Keep API responses compatible with the in-mod package metadata fields:
  - `name`, `version`, `type`, `description`, `downloadUrl`, `sha256`, `artifactType`, `installRoot`

## Open Decisions

- PostgreSQL vs Cosmos DB for metadata.
- App Service vs Container Apps for hosting.
- Whether to require signatures in phase 1 or phase 2.
- Publisher onboarding model (manual allowlist vs self-service invite).
