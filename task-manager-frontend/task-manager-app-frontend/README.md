# Task Manager Frontend

React 19 SPA built with Vite 7. Deployed to Azure Blob Storage and served through Azure Front Door CDN.

## local development

```bash
npm install
npm run dev
```

Starts the Vite dev server at `http://localhost:5173`. See [docs/local-development.md](../../docs/local-development.md) for the full setup (backend, database, etc.).

## build

```bash
npm run build
```

Produces a static bundle in `dist/`. In production this is uploaded to Azure Blob Storage by the `frontend-ci-cd` workflow.
