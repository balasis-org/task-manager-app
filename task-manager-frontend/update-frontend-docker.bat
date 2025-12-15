@echo off
REM Build and start frontend Docker Compose in detached mode
docker compose -p taskmanager -f front-end-compose.yml up --build -d
pause