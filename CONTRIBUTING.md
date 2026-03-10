# Contributing

This project is the work of a single developer and is intended to remain primarily authored by me.
External contributions are not expected, but detailed instructions are provided so that anyone (or future me)
can understand how to run, build, and deploy the project if needed.

## prerequisites

| tool | version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ (`./mvnw` wrapper included) |
| Node.js | 22+ (see `.nvmrc` in the frontend folder) |
| Docker | recent — needed for local SQL Server, Redis, Azurite, MailHog |

## local setup

1. clone the repo
2. start infrastructure containers:
   ```
   docker compose -f task-manager-backend/backend-db-compose.yml up -d
   docker compose -f task-manager-backend/backend-redis-compose.yml up -d
   docker compose -f task-manager-backend/backend-blob-compose.yml up -d
   docker compose -f task-manager-backend/backend-email-compose.yml up -d
   ```
3. run the backend: `./mvnw -pl task-manager-backend spring-boot:run -Dspring-boot.run.profiles=local`
4. run the frontend:
   ```
   cd task-manager-frontend/task-manager-app-frontend
   cp .env.example .env
   npm install
   npm run dev
   ```

see **[docs/iac/README.MD](docs/iac/README.MD)** for full production deployment instructions.

## commit convention

use [Conventional Commits](https://www.conventionalcommits.org/) prefixes:

| prefix | when |
|---|---|
| `feat:` | new feature |
| `fix:` | bug fix |
| `infra:` | Bicep, CI/CD, Docker, GitHub Actions |
| `test:` | k6 scripts or test changes |
| `docs:` | documentation only |
| `refactor:` | code change that neither adds a feature nor fixes a bug |

examples:
```
feat: add task comment editing
fix: prevent N+1 on group member fetch
infra: add Redis health check to deploy-infra
test: add k6 CSRF token replay script
```

Note: You might notice that previous requests in the repo were formed with brackets []
wrapping `feat` or `fix`. DO NOT use those brackets.

## branch strategy

- `main` — production-ready, CI/CD deploys on push
- feature branches — branch off `main`, merge back via PR

## pull requests

1. fill out the **[PR template](.github/pull_request_template.md)**
2. ensure the build passes (`./mvnw clean verify` for backend, `npm run build` for frontend)
3. if your change touches security-sensitive code, run the relevant k6 attack scripts from `task-manager-k6scripts/`
4. keep PRs focused — one concern per PR

## project structure

the codebase enforces compile-time module separation:

- **engine modules** contain domain logic (entities, services, Azure adapters) — no HTTP awareness
- **context modules** contain HTTP delivery (controllers, DTOs, interceptors)
- if a context module imports an engine-internal class (or vice versa), Maven fails the build

respect this boundary when adding code.

## code style

- backend: follow existing patterns — parameterised JPQL, `LEFT JOIN FETCH` for associations, `EnumSet` for role checks
- frontend: functional components, hooks, CSS custom properties for theming
- formatting: see [`.editorconfig`](.editorconfig) for indent/whitespace rules

## reporting issues

use the **[bug report](.github/ISSUE_TEMPLATE/bug_report.md)** or **[feature request](.github/ISSUE_TEMPLATE/feature_request.md)** templates.
