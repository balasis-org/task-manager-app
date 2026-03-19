# Local Development Setup

how to run the full application stack locally. the backend runs as a Spring Boot app with Docker containers for infrastructure dependencies; the frontend runs through Vite's dev server.


## prerequisites

- **Java 21** (JDK) — [Adoptium Temurin](https://adoptium.net/) or equivalent
- **Maven** — install globally or use the included `mvnw` wrapper
- **Node.js 22** — for the frontend dev server
- **Docker** — for MSSQL, Redis, Azurite (blob emulator), and MailHog (email)
- **Azure CLI** — needed to upload default images to local Azurite
- **Azure AD App Registration** — even locally, authentication goes through Azure AD. follow [infrastructure/manual-setup/02-create-auth-app-registration.md](../infrastructure/manual-setup/02-create-auth-app-registration.md) to set up the auth app registration with `localhost` redirect URIs if this hasn't been done already


## 1. start infrastructure containers

all compose files live in `backend/`. run these from that directory:

```bash
# SQL Server (port 1433)
docker compose -p task-manager-app -f backend-db-compose.yml up -d

# Redis for rate limiting (port 6379)
docker compose -p task-manager-app -f backend-redis-compose.yml up -d

# Azurite blob emulator (port 10000)
docker compose -p task-manager-app -f backend-blob-compose.yml up -d

# MailHog SMTP mock (port 1025, web UI at http://localhost:8025)
docker compose -p task-manager-app -f backend-email-compose.yml up -d
```

the `-p task-manager-app` flag puts all containers on the same Docker network so they can reach each other.


## 2. create the database

SQL Server starts empty. create the application database with the correct collation:

```bash
docker exec -it taskmanager_mssql /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U SA -P 'YourPasswordHere' -C \
  -Q "CREATE DATABASE taskmanager_db COLLATE Latin1_General_100_CI_AS_SC;"
```

replace `YourPasswordHere` with whatever password you'll set in your `.env` file (next step).

if you need the Flyway migration profile instead (separate MSSQL instance on port 1434):

```bash
docker compose -p task-manager-app -f backend-flyway-db-compose.yml up -d

docker exec -it taskmanager_mssql_flyway /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U SA -P 'YourFWPasswordHere' -C \
  -Q "CREATE DATABASE taskmanager_flyway_db COLLATE Latin1_General_100_CI_AS_SC;"
```


## 3. configure backend environment

```bash
cd backend
cp .env-example .env
```

open `.env` and fill in the values. the important ones:

- `DB_PASSWORD` — must match the SA password you used in step 2
- `DB_URL` — the JDBC connection string (default points to localhost:1433, usually fine)
- `SPRING_PROFILES_ACTIVE` — set to `dev-mssql` for local development. add `DataLoader` to seed sample data on first startup (e.g. `dev-mssql,DataLoader`)
- `client-id` and `tenant-id` — from the Auth App Registration overview page
- `auth-client-secret` — the client secret from the Auth App Registration
- `auth-redirectUri` — `http://localhost:8081/auth/callback`
- `TASKMANAGER-JWT-SECRET` — any random string, used to sign the app's own JWTs
- `ADMIN-EMAIL` — your Azure AD email address; this account gets admin privileges on first sign-in

the remaining values (Content Safety, Redis connection, etc.) have sensible defaults or are only needed for specific features. see the comments in `.env-example` for details.


## 4. build and run the backend

the backend depends on the shared module, so build that first:

```bash
# from the repo root
mvn -f shared/pom.xml clean install
```

then run the backend.
Note: the easiest way is through your IDE (IntelliJ, VS Code with Java extensions) 
— open the project, find the Spring Boot main class in the `task-manager-context-web-domain` module,
and run it with the `.env` file loaded.(For Intellij 'EnvFile' plugin is recommended)

from the command line:

```bash
# build all backend modules
mvn -f backend/pom.xml clean package -DskipTests

# run the jar
java -jar backend/context/web/domain/target/TaskManager.jar
```

when running from CLI, the `.env` variables need to be exported in your shell first. on Linux/macOS: `export $(cat backend/.env | grep -v '^#' | xargs)`. on Windows, using an IDE that loads `.env` files automatically is simpler.

the backend starts on **port 8080**.


## 5. run the frontend

```bash
cd frontend
npm install
npm run dev
```

the Vite dev server starts at **http://localhost:5173**. the `.env.development` file is already configured to point blob URLs to `http://127.0.0.1:10000/account1/` (local Azurite).


## 6. upload default images (optional)

the application expects some default avatar and group images in blob storage. to load them into local Azurite:

```bash
az storage blob upload-batch \
  --connection-string "DefaultEndpointsProtocol=http;AccountName=account1;AccountKey=key1;BlobEndpoint=http://127.0.0.1:10000/account1;" \
  --source default-images \
  --destination default-images \
  --overwrite
```

run this from the repo root. the `default-images/` folder is gitignored so you need to have it locally.


## compose files reference

all compose files live in `backend/` and share the project name `task-manager-app`:

- **backend-db-compose.yml** — MSSQL 2022 on port 1433 (dev-mssql profile)
- **backend-flyway-db-compose.yml** — MSSQL 2022 on port 1434 (dev-flyway-mssql profile)
- **backend-redis-compose.yml** — Redis 7 Alpine on port 6379
- **backend-blob-compose.yml** — Azurite blob emulator on port 10000
- **backend-email-compose.yml** — MailHog on port 1025 (SMTP) and 8025 (web UI)
- **backend-compose.yml** — the backend itself in a Docker container; useful for testing the Docker image locally, not typically used during day-to-day development


## spring boot profiles

- `dev-mssql` — local Docker MSSQL, connection from `.env`
- `dev-h2` — in-memory H2, used by CI tests (no Docker needed)
- `dev-flyway-mssql` — Flyway-managed schema on a separate MSSQL instance (port 1434)
- `DataLoader` — seeds sample users, groups, and tasks on startup
Note: any other profile are for prod or require additional azure set up.
the maintenance module has its own `.env-example` in `maintenance/blobcleaner/`.


## troubleshooting

**containers lose network after restarting Docker or moving drives:**

```bash
docker network connect task-manager-app_default azurite
docker network connect task-manager-app_default mailhog
```

**database already exists error:**

drop it first, then recreate:

```bash
docker exec -it taskmanager_mssql /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U SA -P 'YourPasswordHere' -C \
  -Q "DROP DATABASE taskmanager_db"
```

**tear down everything (removes containers, images, and volumes):**

```bash
docker compose -p task-manager-app -f backend-db-compose.yml down --rmi all -v
docker compose -p task-manager-app -f backend-redis-compose.yml down --rmi all -v
docker compose -p task-manager-app -f backend-blob-compose.yml down --rmi all -v
docker compose -p task-manager-app -f backend-email-compose.yml down --rmi all -v
```
Note: In some scenario containers might auto restart on boot if you have
limited disc space; make sure you have ~20 gb free
