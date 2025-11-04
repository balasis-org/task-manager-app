Absolutely! Here's a cleaned-up and updated **README.md** based on your current setup, reflecting the two separate containers and including all the actual commands you’re using:

---

# Docker Setup for Task Manager Backend Project

This folder contains the Docker setup for running the **Task Manager backend** and **SQL Server database** locally. It allows developers to quickly start the environment with minimal configuration.

The project consists of two main Docker Compose files:

1. **Database Compose:** `backend-db-compose.yml`

   * Runs a SQL Server container
   * Stores data persistently in a named Docker volume (`mssql-data`)
   * Database can be created manually via `sqlcmd`

2. **Backend Compose:** `backend-compose.yml`

   * Runs the Spring Boot backend container
   * Connects to the SQL Server container
   * Reads environment variables for database connection and Spring profiles

---

## Environment Variables

All environment variables are stored in `.env` and used by Docker Compose. Make sure `.env` exists or copy from `.env-example`.

| Variable                 | Description                                                                                                                              |
| ------------------------ |------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_URL`                 | JDBC URL for SQL Server (e.g., `jdbc:sqlserver://taskmanager_mssql:1433;databaseName=taskmanager_db;encrypt=true;trustServerCertificate=true`) |
| `DB_USERNAME`            | Database username (default: `SA`)                                                                                                        |
| `DB_PASSWORD`            | Database password (must meet SQL Server complexity requirements)                                                                         |
| `SPRING_PROFILES_ACTIVE` | Spring profile for database (`mssql`)                                                                                                    |

These variables are automatically picked up by the backend container.

---

## Steps to Run Locally

### 1. Start the SQL Server container

```bash
docker compose -p taskmanager --env-file docker/.env -f docker/backend-db-compose.yml up -d
```

---

### 2. Create the database (if it does not exist)

```bash
docker compose -p taskmanager -f docker/backend-db-compose.yml exec mssql opt/mssql-tools18/bin/sqlcmd -S localhost -U $DB_USERNAME -P $DB_PASSWORD -C -Q "IF DB_ID('taskmanager_db') IS NULL CREATE DATABASE taskmanager_db;"

```
(replace the variables with your credentials if needed)
**Check the database was created:**

```bash
docker compose -p taskmanager -f docker/backend-db-compose.yml exec mssql opt/mssql-tools18/bin/sqlcmd -S localhost -U $DB_USERNAME -P $DB_PASSWORD -C -Q "SELECT name FROM sys.databases;"
```

> **Note:** The database is stored in the `mssql-data` volume. Removing containers does not delete data.

---

### 3. Build the Spring Boot JAR

From the `task-manager-engine-core` folder:

```bash
mvn clean package -DskipTests
```

This will generate `taskmanager-demo.jar` in the `target` directory, which will be copied into the backend Docker image.

---

### 4. Start the backend container

From the `docker` folder:

```bash
docker compose -p taskmanager -f backend-compose.yml up --build -d
```

* The backend will use environment variables from `.env` for database connection.
* Default container port is mapped `8080:3000` (internal:external).
* API can be accessed via Postman or browser at:

```
http://localhost:8080
```

---

### Notes

* **Port conflicts:** If you also run the backend in IDE on port 3000, it will conflict with the container. Adjust either IDE port or Docker mapping in `backend-compose.yml`.
* **Recreating the database:** Remove the volume if you need a fresh database:

```bash
docker compose -p taskmanager -f docker/backend-db-compose.yml down -v
```

* **Secrets / Environment variables:** `.env` ensures all sensitive info stays outside the Dockerfile.

---

### Quick Start Checklist

1. Ensure `.env` is configured with DB credentials and Spring profile.
2. Start SQL Server container:

```bash
docker compose --env-file docker/.env -f docker/backend-db-compose.yml up -d
```

3. Create `taskmanager_db` database via `sqlcmd`.
4. Build the backend JAR:

```bash
cd task-manager-engine-core
mvn clean package -DskipTests
```

5. Start backend container:

```bash
cd docker
docker compose -f backend-compose.yml up --build -d
```

6. Test APIs using Postman at `http://localhost:8080`.

---