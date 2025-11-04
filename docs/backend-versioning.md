# Backend Versioning Log

This file tracks the versioning of the backend only, independent of the full repository tags.

## Versioning Scheme
- **0.x.y**: Pre-release / development
    - x = minor functionality increment
    - y = patch / small fix
- **1.0.0**: First production-ready backend release

---

## Versions

### 0.1.0 - Initial Dockerized Backend
- Minimal functional backend (APIs only)
- Basic models, services, and auth
- Dockerized for local setup
- No frontend integration
- Commit / Tag: [link or SHA]


### 0.2.0 - Azure Container PoC (TODO)
- Dockerized backend prepared for Azure deployment
- Dummy DB (H2) used for testing container communication
- Commit / Tag: [link or SHA]

### 0.3.0 - Azure SQL Integration (TODO)
- Connects to Azure SQL
- Full container networking and secrets/config
- End-to-end API online
- Commit / Tag: [link or SHA]

---

## Notes
- Frontend or other repo components do **not** affect these backend versions.