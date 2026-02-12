@echo off
REM Build and start frontend Docker Compose in detached mode
docker compose -p task-manager-app -f front-end-compose.yml up --build -d
pause