# Task Manager Application

## Project Description
The Task Manager is a cloud-ready web application designed to manage tasks, roles, and team workflows efficiently. The system supports six roles (Guest, Member, Reviewer, Task Manager, Group Leader, System Administrator) and demonstrates incremental development with containerized deployment.

## Architecture Overview
- **Backend:** Spring Boot REST API, **MS SQL Database (Azure SQL)**, Dockerized.  
- **Frontend:** Single Page Application (SPA), minimal dummy UI initially, communicates via API.  
- **Containerization:** Separate containers for frontend and backend; backend + database in private network, frontend exposed via Azure Front Door.  
- **Cloud Deployment:** Prepared for scaling, Azure AD / Google authentication optional in future iterations.  

*(Architecture diagram placeholder: `docs/architecture.png`)*

## Folder Structure

task-manager-app/
├── backend/
│ ├── docker/ # Docker setup for backend + DB
│ │ ├── .env # ignored in Git
│ │ ├── .env-example # template for environment variables
│ │ ├── backend-compose.yml # backend container
│ │ ├── backend-db-compose.yml # database container
│ │ ├── Dockerfile
│ │ ├── README.md # detailed Docker commands
│ │ └── structure.txt # notes on backend structure
├── frontend/
├── docs/ # Architecture diagrams, screenshots, notes
├── docker-compose.yml # Optional local orchestration placeholder
└── README.md # Project overview


## Roles & Features (Placeholder)
| Role                  | Main Capabilities                         |
|-----------------------|------------------------------------------|
| Guest                 | Browse tasks, limited interaction        |
| Member                | View and update own tasks                 |
| Reviewer              | Review tasks, provide feedback           |
| Task Manager          | Create and assign tasks                   |
| Group Leader          | Monitor team, manage tasks               |
| System Administrator  | System maintenance, technical oversight  |

## Setup Instructions

**Local Development (Backend + DB):**
1. Navigate to `backend/docker/`.
2. Copy `.env-example` to `.env` and fill in the required values.
3. Follow the instructions in `backend/docker/README.md` for running the containers:

```bash
# Start backend container
docker-compose -f backend-compose.yml up --build

# Start database container (requires .env)
docker-compose -f backend-db-compose.yml --env-file .env up

    The .env file is included in .gitignore. Use .env-example as a template.

Frontend:

    Placeholder for SPA setup; will communicate with backend via API.

Deployment:

    Azure deployment files prepared; further configuration required.

Development & Commit History

The project follows structured commit prefixes for clear iterative development:

    [init] — Initial setup

    [feat] — New feature

    [fix] — Bug fix

    [refactor] — Code improvements

    [docs] — Documentation updates

    [setup] — Environment / deployment setup

    [test] — Test implementation

Commit history and screenshots can be used in thesis chapters to show incremental development.
Documentation

All diagrams, screenshots, and iterative notes are stored in the docs/ folder.