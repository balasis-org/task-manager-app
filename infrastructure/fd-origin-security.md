# Front Door Origin Security — Reference

> This is a reference document explaining how Azure Front Door protects the two origin
> types in this architecture. It's not a setup guide — see the [manual-setup guides](manual-setup/)
> for step-by-step instructions.

---

## How Front Door Protects the Two Origin Types

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

---

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

---

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

---

### Defence-in-Depth Summary

Four protection layers, each handled by a different Bicep resource:

| Layer | Mechanism | Bicep resource | Blocks |
|---|---|---|---|
| **Network** | Service tag `AzureFrontDoor.Backend` | `webApp.siteConfig.ipSecurityRestrictions` | All traffic not from an Azure FD IP |
| **Instance** | `X-Azure-FDID` header match | Same restriction block | Requests from other customers' FD profiles |
| **Cryptographic** | FD origin auth (MI → Bearer token) + Easy Auth | `fdOriginGroupApi.authentication` + `webAppAuth` | IP spoofing (requires valid Entra ID token) |
| **Blob origin** | Origin auth (MI → Bearer token) + IAM | `fdOriginGroupBlob.authentication` + `fdBlobRoleAssignment` | Keeps blob storage fully private |

The origin authentication feature is currently in preview. The Bicep template uses the `@2025-09-01-preview` API version for the API origin group. Front Door Premium SKU supports Private Link to App Service (eliminating the public endpoint entirely) but costs ~$330/month vs ~$35/month for Standard.
