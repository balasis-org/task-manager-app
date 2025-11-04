Both from docker folder

Dev:
docker compose -f front-end-compose.yml -p taskmanager up -d --build

Prod:

docker compose -f front-end-compose-build.yml build

Tag it:docker tag docker-frontend anstaskmanageracr-g3bwdtdrb8h9bfem.azurecr.io/taskmanager-frontend:latest
Login to azure acr : az acr login --name anstaskmanageracr

Push it : docker push anstaskmanageracr-g3bwdtdrb8h9bfem.azurecr.io/taskmanager-frontend:latest
