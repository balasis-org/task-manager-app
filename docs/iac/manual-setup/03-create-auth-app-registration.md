# 03 — Create Auth App Registration (One-Time)

> Run once with an Owner/Global Admin account.
> This is a **separate** registration from the CI/CD service principal (`taskmanager-cicd`).

## Purpose

The application uses **Azure AD (Entra ID) OAuth 2.0 Authorization Code** flow
for user authentication. This requires an App Registration that:

1. Holds the **client ID** and **tenant ID** used by the Spring Boot backend
   to build authorization URLs and exchange codes for tokens
2. Stores a **client secret** used server-side for the token exchange
3. Defines the **redirect URIs** Azure AD sends users back to after login
4. Has an **Application ID URI** that serves as the audience identifier for
   token validation (`decodeIdToken` verifies `aud` claim matches the client ID)

---

## Option A: Azure Portal

### Step 1 — Register the Application

1. Azure Portal → **App registrations** → **+ New registration**
2. Name: `TaskManager-Auth` (or similar — descriptive, not used in code)
3. Supported account types: **Single tenant**
   (Current restrictions of the backend capping to 10k users combined with the 
 tier based access restrictions with no NonAdmin-way to update to any non-FREE tier
 makes the choice of multitenant+MSA also a viable choice.
 Later one could risk to update the backend capping and enable payment.(Risk investment mode))
4. Redirect URI: leave blank for now (added in Step 3)
5. Click **Register**
6. Note from the **Overview** page:
   - **Application (client) ID** → stored as `TASKMANAGER-AUTH-CLIENT-ID`
   - **Directory (tenant) ID** → stored as `TASKMANAGER-AUTH-TENANT-ID`

### Step 2 — Create Client Secret

1. Left nav → **Certificates & secrets** → **+ New client secret**
2. Description: e.g. `backend-auth`
3. Expiry: 12 or 24 months (must rotate before expiry)
4. Copy the **Value** immediately → stored as `TASKMANAGER-AUTH-CLIENT-SECRET`

### Step 3 — Add Redirect URIs

1. Left nav → **Authentication** → **+ Add a platform** → **Web**
2. Add these redirect URIs:
   - `https://www.myteamtasks.net/auth/callback` (production, custom domain)
   - `https://<fd-endpoint>.azurefd.net/auth/callback` (production, FD endpoint — add after first deploy)
   - `http://localhost:5173/auth/callback` (local frontend dev server)
   - `http://localhost:8081/auth/callback` (local backend)

the FD endpoint hostname is output by the Bicep deployment as `frontDoorEndpointHostName`. you can add it after the first deploy.

### Step 3b — Configure Authentication Settings

still in **Authentication**, switch to the **Settings** tab:

1. Under "Implicit grant and hybrid flows", check **ID tokens** and leave **Access tokens** unchecked
2. Leave **Allow public client flows** disabled
3. Leave **Front-channel logout URL** empty
4. Click **Save**

### Step 4 — Set Application ID URI

1. Left nav → **Expose an API** → **Set** (next to Application ID URI)
2. Accept the default: `api://<client-id>` — e.g. `api://7b429825-f7cf-4971-b9b7-36c491c8ed3e`
3. Click **Save**

> The Application ID URI is used as the **audience claim** in Azure AD tokens.
> The backend validates that the `aud` field in the `id_token` matches the
> `clientId`. No scopes or authorized client applications need to be defined —
> the app uses the standard `openid profile email User.Read` delegated scopes.

### Step 5 — Store Secrets in Key Vault

After the Bicep deployment creates the Key Vault, manually add these seven secrets:

1. `TASKMANAGER-AUTH-CLIENT-ID` — Application (client) ID from the App Registration overview
2. `TASKMANAGER-AUTH-TENANT-ID` — Directory (tenant) ID from the App Registration overview
3. `TASKMANAGER-AUTH-CLIENT-SECRET` — the client secret value from Step 2
4. `TASKMANAGER-AUTH-REDIRECT-URI` — `https://www.myteamtasks.net/auth/callback` (your production redirect URI)
5. `ADMIN-EMAIL` — the admin user's email address (this user gets auto-promoted to admin on first sign-in)
6. `TASKMANAGER-JWT-SECRET` — a random 256-bit key in base64 (the signing key for the app's own JWTs)
7. `TASKMANAGER-EMAIL-SENDER-ADDRESS` — your verified ACS sender email (ACS → Email → Provisioned domain)

the Key Vault ends up with 17 secrets total: 10 auto-populated by Bicep (DB connection, blob keys, Redis, Content Safety, ACS, App Insights) and 7 manual (the ones above). the maintenance Container Apps jobs read from the same Key Vault at runtime using the same Managed Identity.

## Option B: Azure CLI

> **Important:** The Azure Portal auto-creates a Service Principal when you
> register an app. The CLI only creates the App Registration (the definition),
> so you must manually create the Service Principal with `az ad sp create`.

```bash
# login with your Owner account
az login

# Step 1: Create the app registration (single-tenant is the default) 
az ad app create --display-name "TaskManager-Auth" --sign-in-audience AzureADMyOrg

# note the appId from output -> this is the Client ID
# note the id (object ID) -> needed by some commands
# the tenant ID is in the az login output

# create the service principal (Portal does this automatically — CLI does NOT)
az ad sp create --id <APP_ID>

# Step 2: Create a client secret 
# copy the password from the output immediately (you won't see it again)
az ad app credential reset \
  --id <APP_ID> \
  --display-name "backend-auth"

#  Step 3: Add redirect URIs 
# add the FD endpoint URI after the first Bicep deploy (output: frontDoorEndpointHostName)
az ad app update --id <APP_ID> \
  --web-redirect-uris \
    "https://www.myteamtasks.net/auth/callback" \
    "http://localhost:5173/auth/callback" \
    "http://localhost:8081/auth/callback"

# Step 3b: Enable ID token (implicit grant) 
# access tokens left disabled  only ID tokens are needed for the OAuth code flow
az ad app update --id <APP_ID> \
  --enable-id-token-issuance true

# Step 4: Set Application ID URI (audience for token validation) 
az ad app update --id <APP_ID> --identifier-uris "api://<APP_ID>"
#-------------------------Proceed to step 5 only after bicep deployment) --------------------------------


# Step 5: Store secrets in Key Vault
KV_NAME="<your-keyvault-name>"

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

---

## How FD Protects the Two Origin Types

The Front Door profile uses **different security mechanisms** for its two origin types:

### Blob Storage Origin (Origin Authentication + IAM)

```
Front Door ──[Bearer token (MI, scope: storage.azure.com)]──→ Blob Storage (validates token + RBAC)
```

1. The FD profile has a **System-Assigned Managed Identity**
2. FD origin authentication (preview) acquires an Entra ID token with scope
   `https://storage.azure.com/.default` and sends it as `Authorization: Bearer` to blob storage
3. An IAM role assignment grants `Storage Blob Data Reader` to FD's MI on the storage account
   — this is the **authorisation** layer (what FD is allowed to do)
4. All blob containers are **private** (`publicAccess: None`)
5. Both layers are required: origin auth = authentication (who you are), RBAC = authorisation (what you can do)

**Bicep:** `fdOriginGroupBlob` uses `@2025-09-01-preview` with `authentication.scope: 'https://storage.azure.com/.default'`

### Web App Origin (Three-Layer Defence-in-Depth)

```
Front Door ──[1. service tag + 2. FDID header + 3. Bearer token (MI)]──→ App Service (Easy Auth validates)
```

1. **Network (IP):** App Service `ipSecurityRestrictions` allows ONLY the `AzureFrontDoor.Backend`
   **service tag** — any request not from an Azure Front Door IP is rejected (HTTP 403)
2. **Instance (Header):** The restriction also checks the `X-Azure-FDID` **header** matches this
   specific FD profile's ID — prevents requests from OTHER customers' FD profiles
3. **Cryptographic (Token):** FD origin authentication (preview) uses FD's system-assigned MI to
   acquire an Entra ID token (scope: `api://<client-id>/.default`) and sets it as
   `Authorization: Bearer <token>` on every forwarded request. Easy Auth on the App Service
   validates the token's audience, issuer, and tenant — **cryptographic proof** that the request
   came from this specific Front Door instance, defeating IP spoofing

### Easy Auth Configuration (App Service Authentication)

Easy Auth (`authsettingsV2`) is configured on the App Service in **Require Authentication**
mode, working in tandem with Front Door's origin authentication. This is NOT used
for end-user login — the Spring Boot app manages its own OAuth 2.0 code flow and
JWT cookies. Instead, Easy Auth serves as the **cryptographic anti-spoofing layer**.

**How the token flow works:**

1. Front Door's origin group has origin authentication enabled (preview feature)
2. FD uses its **system-assigned managed identity** to acquire an Entra ID access
   token with scope `api://<client-id>/.default`
3. FD sets the token as `Authorization: Bearer <token>` on every forwarded request
4. Easy Auth intercepts the request, validates the Bearer token (audience, issuer, tenant)
5. Since the token is valid, the request passes through to the Spring Boot app
6. The Spring Boot app handles its own user auth via cookies (ignores the Authorization header)

> **Important:** FD **overwrites** any existing `Authorization` header with its own
> MI-acquired token. The Spring Boot app uses cookies, not Authorization headers,
> so this does not conflict.

**Portal configuration used:**

- Restrict access: **Require authentication**
- Unauthenticated requests: **HTTP 302 Found redirect** to **Microsoft**
- Identity provider: Microsoft (Entra ID), using the `TaskManager-Auth` app registration
- Client application requirement: allow requests only from this application itself
- Tenant requirement: allow requests only from the issuer tenant
- Token store: enabled
- Issuer URL: `https://sts.windows.net/<tenant-id>/v2.0`

**Why Require Authentication mode works here:**

- FD **always** sends a valid Bearer token (via origin authentication) → Easy Auth validates
  it → request passes through to Spring Boot
- The 302 redirect only fires for requests **without** a Bearer token — but the service tag
  already blocks all non-FD traffic, so direct requests never reach the app
- If origin authentication were misconfigured and FD stopped sending tokens, the 302 redirect
  would prevent any request from reaching the backend — **fail-closed** behaviour

**Bicep resource:** `webAppAuth` (`Microsoft.Web/sites/config` / `authsettingsV2`) — see `main.bicep`

> **Note:** The `MICROSOFT_PROVIDER_AUTHENTICATION_SECRET` app setting uses a
> Key Vault reference (`@Microsoft.KeyVault(...)`) to read the client secret from
> `TASKMANAGER-AUTH-CLIENT-SECRET`. This resolves at runtime — the manual KV
> secret must exist before Easy Auth can validate tokens.

### Defence-in-Depth Summary

four protection layers, each handled by a different Bicep resource:

1. **Network** — service tag `AzureFrontDoor.Backend` (`webApp.siteConfig.ipSecurityRestrictions`) blocks all traffic not coming from an Azure Front Door IP
2. **Instance** — `X-Azure-FDID` header match (same restriction block) rejects requests from other customers' FD profiles
3. **Cryptographic** — FD origin auth (MI → Bearer token) + Easy Auth validates (`fdOriginGroupApi.authentication` + `webAppAuth`) — defeats IP spoofing by requiring a valid Entra ID token
4. **Blob origin** — origin auth (MI → Bearer token) + IAM `Storage Blob Data Reader` (`fdOriginGroupBlob.authentication` + `fdBlobRoleAssignment`) — keeps blob storage fully private

the origin authentication feature is currently in preview. the Bicep template uses the `@2025-09-01-preview` API version for the API origin group. Front Door Premium SKU supports Private Link to App Service (eliminating the public endpoint entirely) but costs ~$330/month vs ~$35/month for Standard.

---
