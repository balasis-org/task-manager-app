# 02 — Create Auth App Registration

> Run once with an Owner/Global Admin account.
> This is a **separate** registration from the CI/CD service principal created in guide 01.

## purpose

The application uses **Azure AD (Entra ID) OAuth 2.0 Authorization Code** flow
for user authentication. This App Registration:

1. Holds the **client ID** and **tenant ID** used by the Spring Boot backend
   to build authorization URLs and exchange codes for tokens
2. Stores a **client secret** used server-side for the token exchange
3. Defines the **redirect URIs** Azure AD sends users back to after login
4. Has an **Application ID URI** that serves as the audience identifier for
   token validation (`decodeIdToken` verifies `aud` claim matches the client ID)

> **Tenant choice:** Single tenant is the default and recommended setting. The current
> backend restrictions (10k user cap, tier-based access with no self-serve upgrade)
> make multi-tenant + personal Microsoft accounts a viable future option if you
> later lift the cap and enable a payment flow.

---

## Option A: Azure Portal

### Step 1 — Register the Application

1. Azure Portal → **App registrations** → **+ New registration**
2. Name: `TaskManager-Auth` (descriptive — not used in code)
3. Supported account types: **Single tenant**
4. Redirect URI: leave blank for now (added in step 3)
5. Click **Register**
6. Note from the **Overview** page:
   - **Application (client) ID** → will become `TASKMANAGER-AUTH-CLIENT-ID`
   - **Directory (tenant) ID** → will become `TASKMANAGER-AUTH-TENANT-ID`

### Step 2 — Create Client Secret

1. Left nav → **Certificates & secrets** → **+ New client secret**
2. Description: e.g. `backend-auth`
3. Expiry: 12 or 24 months (must rotate before expiry)
4. Copy the **Value** immediately → will become `TASKMANAGER-AUTH-CLIENT-SECRET`

### Step 3 — Add Redirect URIs

1. Left nav → **Authentication** → **+ Add a platform** → **Web**
2. Add these redirect URIs:
   - `https://www.myteamtasks.net/auth/callback` (production — custom domain)
   - `http://localhost:5173/auth/callback` (local frontend dev server)
   - `http://localhost:8081/auth/callback` (local backend)
3. The Front Door endpoint URI (`https://<fd-endpoint>.azurefd.net/auth/callback`) is added after the first Bicep deployment — see [guide 03](03-post-deployment-setup.md)
# Note — in case of arena set up you will need to come back to this app registration in order to add the FD url+callback URI


### Step 4 — Configure Authentication Settings

Still in **Authentication**:

1. Under "Implicit grant and hybrid flows", check **ID tokens** (leave **Access tokens** unchecked)
2. Leave **Allow public client flows** disabled
3. Leave **Front-channel logout URL** empty
4. Click **Save**

### Step 5 — Set Application ID URI

1. Left nav → **Expose an API** → **Set** (next to Application ID URI)
2. Accept the default: `api://<client-id>` — e.g. `api://7b429825-f7cf-4971-b9b7-36c491c8ed3e`
3. Click **Save**

> The Application ID URI is used as the **audience claim** in Azure AD tokens.
> The backend validates that the `aud` field in the `id_token` matches the
> `clientId`. No scopes or authorized client applications need to be defined —
> the app uses the standard `openid profile email User.Read` delegated scopes.

---

## Option B: Azure CLI

> **Important:** The Azure Portal auto-creates a Service Principal when you
> register an app. The CLI only creates the App Registration (the definition),
> so you must manually create the Service Principal with `az ad sp create`.

```bash
# login with your Owner account
az login

# Step 1: Create the app registration (single-tenant is the default)
az ad app create --display-name "TaskManager-Auth" --sign-in-audience AzureADMyOrg

# note the appId from output → this is the Client ID
# note the id (object ID) → needed by some commands
# the tenant ID is in the az login output

# create the service principal (Portal does this automatically — CLI does NOT)
az ad sp create --id <APP_ID>

# Step 2: Create a client secret (copy the password from output immediately)
az ad app credential reset \
  --id <APP_ID> \
  --display-name "backend-auth"

# Step 3: Add redirect URIs
# (add the FD endpoint URI after first Bicep deploy — see guide 03)
az ad app update --id <APP_ID> \
  --web-redirect-uris \
    "https://www.myteamtasks.net/auth/callback" \
    "http://localhost:5173/auth/callback" \
    "http://localhost:8081/auth/callback"

# Step 4: Enable ID token issuance
# access tokens left disabled — only ID tokens are needed for the OAuth code flow
az ad app update --id <APP_ID> \
  --enable-id-token-issuance true

# Step 5: Set Application ID URI (audience for token validation)
az ad app update --id <APP_ID> --identifier-uris "api://<APP_ID>"
```

---

## what to save

After this guide, save these three values — you'll add them to Key Vault after Bicep creates it ([guide 03](03-post-deployment-setup.md)):

| Value | Key Vault secret name |
|---|---|
| Application (client) ID | `TASKMANAGER-AUTH-CLIENT-ID` |
| Directory (tenant) ID | `TASKMANAGER-AUTH-TENANT-ID` |
| Client secret value | `TASKMANAGER-AUTH-CLIENT-SECRET` |

If you also want Azure AD authentication in local development, add these to your backend and maintenance `.env` files:

```
tenant-id=<tenant-id>
auth-client-secret=<client-secret>
auth-redirectUri=http://localhost:5173/auth/callback
```

> **Next:** Return to the [IaC README](../README.MD) for GitHub setup and infrastructure deployment.
> The Key Vault doesn't exist yet — secrets are added in [guide 03](03-post-deployment-setup.md) after Bicep runs.
