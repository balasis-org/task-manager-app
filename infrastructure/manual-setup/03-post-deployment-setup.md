# 03 — Post-Deployment Setup

> Run once after the first Bicep deployment creates your Azure resources.
> The Key Vault, ACS instance, and Front Door endpoint now exist — this guide finishes
> the configuration that depends on them.

---

## Step 1 — Seed Key Vault Secrets

The Key Vault has 17 secrets total: **10 auto-populated by Bicep** (DB connection string, blob keys, Redis, Content Safety, ACS connection string, App Insights) and **7 that you add manually**:

### Option A: Azure Portal

Go to your Key Vault → **Secrets** → **+ Generate/Import** for each:

| Secret name | Value | Source |
|---|---|---|
| `TASKMANAGER-AUTH-CLIENT-ID` | Application (client) ID | Auth App Registration overview ([guide 02](02-create-auth-app-registration.md)) |
| `TASKMANAGER-AUTH-TENANT-ID` | Directory (tenant) ID | Auth App Registration overview ([guide 02](02-create-auth-app-registration.md)) |
| `TASKMANAGER-AUTH-CLIENT-SECRET` | Client secret value | Auth App Registration ([guide 02](02-create-auth-app-registration.md), step 2) |
| `TASKMANAGER-AUTH-REDIRECT-URI` | `https://www.myteamtasks.net/auth/callback` | Your production redirect URI |
| `ADMIN-EMAIL` | Admin user's email address | This user gets auto-promoted to admin on first sign-in |
| `TASKMANAGER-JWT-SECRET` | Random 256-bit base64 key | Generate: `openssl rand -base64 32` |
| `TASKMANAGER-EMAIL-SENDER-ADDRESS` | Verified ACS sender email | See note below |

### Option B: Azure CLI

```bash
KV_NAME="<your-keyvault-name>"   # from Bicep outputs or Azure Portal

az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-AUTH-CLIENT-ID" --value "<client-id>"

az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-AUTH-TENANT-ID" --value "<tenant-id>"

az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-AUTH-CLIENT-SECRET" --value "<client-secret>"

az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-AUTH-REDIRECT-URI" \
  --value "https://www.myteamtasks.net/auth/callback"

az keyvault secret set --vault-name $KV_NAME \
  --name "ADMIN-EMAIL" --value "<admin-email>"

# generate a 256-bit base64 key: openssl rand -base64 32
az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-JWT-SECRET" --value "<base64-256-bit-key>"

az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-EMAIL-SENDER-ADDRESS" --value "<acs-sender-email>"
```

> **ACS sender address:** After Bicep creates the ACS instance, go to **Azure Communication
> Services** → your resource → **Email** → **Provisioned domains**. The sender address is
> listed there (e.g. `DoNotReply@<domain>`). If you haven't provisioned an email domain yet,
> do so first under **Provision domains**, then use the sender address shown.

> **`MICROSOFT_PROVIDER_AUTHENTICATION_SECRET`:** The App Service has an app setting that
> references `TASKMANAGER-AUTH-CLIENT-SECRET` via a Key Vault reference
> (`@Microsoft.KeyVault(...)`). This resolves at runtime — the manual KV secret **must exist**
> before Easy Auth can validate Front Door's origin authentication tokens.

---

## Step 2 — Add Front Door Redirect URI

The Front Door endpoint hostname is output by the Bicep deployment (output name: `frontDoorEndpointHostName`). You can also find it in the Azure Portal under **Front Door and CDN profiles** → your profile → **Endpoints**.

Add this redirect URI to the Auth App Registration:

### Option A: Azure Portal

1. **App registrations** → `TaskManager-Auth` → **Authentication**
2. Under **Web** redirect URIs, click **Add URI**
3. Add: `https://<fd-endpoint>.azurefd.net/auth/callback`
4. Click **Save**

### Option B: Azure CLI

```bash
# az ad app update --web-redirect-uris REPLACES all URIs — include every one you want to keep
az ad app update --id <AUTH_APP_ID> \
  --web-redirect-uris \
    "https://www.myteamtasks.net/auth/callback" \
    "https://<fd-endpoint>.azurefd.net/auth/callback" \
    "http://localhost:5173/auth/callback" \
    "http://localhost:8081/auth/callback"
```

---

## Step 3 — Upload Default Images

The application uses default images stored in Azure Blob Storage. Upload them once:

```bash
# authenticate (use the CI/CD SP or az login interactively)
az login --service-principal -u <CLIENT_ID> -p <CLIENT_SECRET> --tenant <TENANT_ID>

# upload (replace <STORAGE_ACCOUNT> with the value from deployment outputs or GitHub env vars)
az storage blob upload-batch \
  --account-name <STORAGE_ACCOUNT> \
  --source default-images \
  --destination default-images \
  --auth-mode login \
  --overwrite
```

for local development with Azurite (see also [local-development.md](../../local-development.md)):

```bash
az storage blob upload-batch \
  --connection-string "DefaultEndpointsProtocol=http;AccountName=account1;AccountKey=key1;BlobEndpoint=http://127.0.0.1:10000/account1;" \
  --source default-images \
  --destination default-images \
  --overwrite
```

---

## verification checklist

After completing this guide, verify:

- [ ] Key Vault shows 17 secrets (10 auto + 7 manual)
- [ ] Auth App Registration has 4 redirect URIs (production domain, FD endpoint, localhost:5173, localhost:8081)
- [ ] Default images container exists in Blob Storage with uploaded files
- [ ] The backend App Service starts successfully (it reads Key Vault secrets at startup — if any are missing, the app fails to start with a clear error)
- [ ] Front Door health probes show the backend origin as healthy
