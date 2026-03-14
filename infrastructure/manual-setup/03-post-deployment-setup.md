# 03 — Post-Deployment Setup

> Run once after the first Bicep deployment creates your Azure resources.
> The Key Vault, ACS instance, and Front Door endpoint now exist — this guide finishes
> the configuration that depends on them.

---

## Step 1 — Seed Key Vault Secrets

The Key Vault has 19 secrets total: **11 auto-populated by Bicep** (DB connection string, blob keys, Redis, Content Safety, ACS endpoints x2, App Insights) and **8 that you add manually**:

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
| `TASKMANAGER-EMAIL-SENDER-ADDRESS` | Verified ACS sender email (user-facing) | See note below |
| `TASKMANAGER-EMAIL-ADMIN-SENDER-ADDRESS` | Verified ACS sender email (admin alerts) | See note below |

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

az keyvault secret set --vault-name $KV_NAME \
  --name "TASKMANAGER-EMAIL-ADMIN-SENDER-ADDRESS" --value "<admin-acs-sender-email>"
```

> **ACS sender addresses:** Bicep provisions **two** ACS instances — one for user-facing emails and one for admin alerts — each with its own Azure-managed email domain.
> For each ACS resource, go to **Azure Communication Services** → the resource → **Email** → **Provisioned domains** → **AzureManagedDomain**. The sender address is listed under **MailFrom addresses** (e.g. `DoNotReply@<guid>.azurecomm.net`). Copy each value into the corresponding Key Vault secret:
> - User ACS (`<projectName>-acs`) → `TASKMANAGER-EMAIL-SENDER-ADDRESS`
> - Admin ACS (`<projectName>-acs-admin`) → `TASKMANAGER-EMAIL-ADMIN-SENDER-ADDRESS`

> **`MICROSOFT_PROVIDER_AUTHENTICATION_SECRET`:** The App Service has an app setting that
> references `TASKMANAGER-AUTH-CLIENT-SECRET` via a Key Vault reference
> (`@Microsoft.KeyVault(...)`). This resolves at runtime — the manual KV secret **must exist**
> before Easy Auth can validate Front Door's origin authentication tokens.

---

## Step 2 — DNS Configuration (GoDaddy)

Two DNS records connect your custom domain to Azure resources.

### 2a — Front Door CNAME

Bicep creates the custom domain resource on Front Door when `customDomainHost` is set (production only). You must add the DNS record in GoDaddy to point to it:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| CNAME | www | `<fd-endpoint>.azurefd.net` | 1 Hour |

Front Door will auto-provision a managed TLS certificate once the CNAME resolves. This can take 10-15 minutes.

Also add the **validation TXT** record that Azure shows in the Front Door custom domain setup:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| TXT | _dnsauth.www | `<validation-token-from-azure>` | 1 Hour |

### 2b — ACS Email Custom Domain (optional — branded sender address)

By default, ACS sends from an Azure-managed domain (`DoNotReply@<guid>.azurecomm.net`). To send from a custom subdomain (e.g. `DoNotReply@mail.myteamtasks.net`):

1. **Azure Portal → Communication Services → your ACS → Email → Domains → Add custom domain**
2. Enter: `mail.myteamtasks.net`
3. Azure gives you a **TXT verification record** — add it in GoDaddy:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| TXT | mail | `ms-domain-verification=<value-from-azure>` | 1 Hour |

4. Wait 15-20 minutes for verification to complete (check the domain status in Azure Portal)
5. After verification, Azure shows **SPF** and **DKIM** records. Add these in GoDaddy:

| Type | Name | Value | Purpose |
|------|------|-------|--------|
| TXT | mail | `v=spf1 include:spf.protection.outlook.com -all` | SPF — authorises Azure to send on behalf of your domain |
| CNAME | selector1-azurecomm-prod-net._domainkey.mail | `<value-from-azure>` | DKIM — cryptographic email signing key 1 |
| CNAME | selector2-azurecomm-prod-net._domainkey.mail | `<value-from-azure>` | DKIM — cryptographic email signing key 2 |

6. Wait for SPF and DKIM to show as **Verified** in the Azure Portal
7. Add a **MailFrom address** (e.g. `DoNotReply@mail.myteamtasks.net`)
8. Link this domain to your ACS resource (Domains → your domain → Link to Communication Service)
9. Update the Key Vault secret `TASKMANAGER-EMAIL-SENDER-ADDRESS` with the new sender address

### 2c — Admin ACS Email Custom Domain (optional — branded admin alerts)

Bicep provisions a second ACS instance (`<projectName>-acs-admin`) dedicated to critical admin alerts. By default it uses an Azure-managed domain. To brand admin emails (e.g. `DoNotReply@alerts.myteamtasks.net`):

1. Repeat the same steps as 2b above, but for the **admin** ACS resource
2. Use a different subdomain: `alerts.myteamtasks.net`
3. Add the TXT, SPF, and DKIM records in GoDaddy (same process, different subdomain name)
4. After verification, add a MailFrom address (e.g. `DoNotReply@alerts.myteamtasks.net`)
5. Link the domain to the admin ACS resource
6. Update the Key Vault secret `TASKMANAGER-EMAIL-ADMIN-SENDER-ADDRESS` with the admin sender address

> **Note:** Each custom email domain can only be linked to ONE ACS resource. The user-facing ACS uses `mail.myteamtasks.net` and the admin ACS uses `alerts.myteamtasks.net`. Arena environments should use the default Azure-managed domains.

> **SPF** (Sender Policy Framework) tells receiving mail servers "Azure is authorised to send email for this domain." Without it, emails may land in spam.
> **DKIM** (DomainKeys Identified Mail) adds a cryptographic signature to outgoing emails, proving they haven't been tampered with in transit.

---

## Step 3 — Add Front Door Redirect URI

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

## Step 4 — Upload Default Images

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

- [ ] Key Vault shows 19 secrets (11 auto + 8 manual)
- [ ] Front Door CNAME resolves (`nslookup www.myteamtasks.net` → azurefd.net)
- [ ] Front Door managed TLS certificate is provisioned (check Azure Portal)
- [ ] ACS email custom domain shows as Verified (if configured)
- [ ] Auth App Registration has 4 redirect URIs (production domain, FD endpoint, localhost:5173, localhost:8081)
- [ ] Default images container exists in Blob Storage with uploaded files
- [ ] The backend App Service starts successfully (it reads Key Vault secrets at startup — if any are missing, the app fails to start with a clear error)
- [ ] Front Door health probes show the backend origin as healthy
