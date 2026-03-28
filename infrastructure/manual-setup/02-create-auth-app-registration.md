# 02 — Create Auth App Registration

> Run once with an Owner/Global Admin account.
> This is a **separate** registration from the CI/CD service principal created in guide 01.

## purpose

This App Registration serves **two independent purposes** on the same Entra ID object:

**A — OAuth 2.0 user login (Spring Boot backend code):**
1. Holds the **client ID** and **tenant ID** used by the Spring Boot backend
   to build authorization URLs and exchange codes for tokens
2. Stores a **client secret** used server-side for the token exchange
3. Defines the **redirect URIs** Azure AD sends users back to after login

**B — Easy Auth / Front Door origin authentication (Azure infrastructure):**
4. Has an **Application ID URI** (`api://<client-id>`) that serves as the
   audience for Easy Auth on the App Service — Front Door's managed identity
   requests a token scoped to `api://<client-id>/.default`, and Easy Auth
   validates that the token's audience matches

> Easy Auth is **not** involved in user login — it only validates that incoming
> requests carry a valid Front Door MI token. User sign-in is handled entirely
> by the Spring Boot OAuth 2.0 Authorization Code flow (purpose A above).

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
   > **Why:** The backend uses the OAuth 2.0 hybrid flow (`response_type=code id_token`).
   > Microsoft returns a signed `id_token` in the redirect alongside the authorization code.
   > The backend verifies this front-channel `id_token` (signature, audience, nonce, `c_hash`)
   > **before** calling Microsoft's `/token` endpoint — this proves the user actually
   > authenticated with Microsoft at zero API cost, blocking forged-code spam.
   > Without this checkbox enabled, Microsoft will reject the hybrid request.
2. Leave **Allow public client flows** disabled
3. Leave **Front-channel logout URL** empty
4. Click **Save**

### Step 5 — Set Application ID URI

1. Left nav → **Expose an API** → **Set** (next to Application ID URI)
2. Accept the default: `api://<client-id>` — e.g. `api://7b429825-f7cf-4971-b9b7-36c491c8ed3e`
3. Click **Save**

> The Application ID URI is consumed by **Easy Auth** on the App Service:
> Bicep configures `allowedAudiences: ['api://<client-id>']` in the
> `authsettingsV2` resource, and Front Door's origin group requests tokens
> scoped to `api://<client-id>/.default`. This is how FD proves its identity
> to the App Service — **not** related to user login.
>
> The backend's OAuth 2.0 flow validates `id_token` audience against the plain
> `clientId` (not the `api://` URI). No custom scopes or authorized client
> applications need to be defined — the app uses the standard
> `openid profile email User.Read` delegated permissions.

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

# Step 4: Enable ID token issuance (required for hybrid flow — response_type=code id_token)
# the backend verifies the front-channel id_token before calling /token,
# proving the user authenticated with Microsoft at zero API cost
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
