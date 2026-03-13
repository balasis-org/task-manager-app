# 01 — Create CI/CD Service Principal

> Run once with an Owner/Global Admin account.
> This creates the identity that GitHub Actions (and optionally your local CLI) uses to deploy Azure resources.

## what you're creating

An **App Registration** (the definition) + **Service Principal** (the identity) + **client secret** (the credential) in Azure AD (Entra ID), with two subscription-level roles:

- **Contributor** — lets it create and destroy resource groups and all Azure resources
- **User Access Administrator** — lets it create RBAC role assignments inside the Bicep template (Key Vault, Storage, Cognitive Services, ACS, Front Door)

without User Access Administrator the deployment fails at the RBAC role-assignment resources with `AuthorizationFailed`.

> You may want to repeat this guide twice if you want separate service principals for manual use and GitHub CI/CD, but using the same credentials for both is simpler and recommended.

---

## Option A: Azure Portal

### create the App Registration

1. Azure Portal → search "App registrations" → **App registrations** under Services → **+ New registration**
2. Name: e.g. `taskmanager-cicd`
3. Supported account types: **Single tenant** (important — avoids cross-tenant security issues)
4. Click **Register** (skip the redirect URI)
5. On the Overview page, note:
   - **Application (client) ID**
   - **Directory (tenant) ID**
6. Go to **Certificates & secrets** → **+ New client secret**
7. Copy the secret **Value** immediately (you won't see it again)

### assign subscription roles

1. Go to **Subscriptions** → your subscription → **Access control (IAM)**
2. Click **+ Add** → **Add role assignment**
3. Role: **Contributor** → Members tab → **User, group, or service principal** → search for your app name (e.g. `taskmanager-cicd`) → select → **Review + assign**
4. **Repeat steps 2-3** for the **User Access Administrator** role

---

## Option B: Azure CLI

> **Important:** The Azure Portal auto-creates a Service Principal when you
> register an app. The CLI only creates the App Registration (the definition),
> so you must manually create the Service Principal (the actual identity that
> authenticates and receives role assignments) with `az ad sp create`.

```bash
# login with your Owner account (choose your subscription)
az login

# create the app registration (single-tenant is the default)
az ad app create --display-name taskmanager-cicd

# note the appId from output (this is the Client ID)

# create the service principal (Enterprise Application)
# Portal does this automatically — CLI does NOT
az ad sp create --id <APP_ID>

# create a client secret
# (you may also name it using --display-name <name>)
# Note: we could also use a certificate (personal preference of avoiding self-signed ones
#       and we want to avoid extra charges for CA — so we stick with secrets for now)
az ad app credential reset --id <APP_ID>

# assign Contributor — create/destroy resources
# (subscription ID is in the az login output)
az role assignment create \
  --assignee <APP_ID> \
  --role "Contributor" \
  --scope /subscriptions/<SUBSCRIPTION_ID>

# assign User Access Administrator — create RBAC assignments in Bicep
az role assignment create \
  --assignee <APP_ID> \
  --role "User Access Administrator" \
  --scope /subscriptions/<SUBSCRIPTION_ID>
```

---

## what to save

After completing this guide, you should have four values:

| Value | Where it goes |
|---|---|
| Application (client) ID | GitHub secret `AZURE_CLIENT_ID` · `.env` file |
| Client secret value | GitHub secret `AZURE_CLIENT_SECRET` · `.env` file |
| Directory (tenant) ID | GitHub secret `AZURE_TENANT_ID` · `.env` file |
| Subscription ID | GitHub secret `AZURE_SUBSCRIPTION_ID` · `.env` file |

Save these somewhere secure. You'll add them as GitHub secrets in the next phase (see the [IaC README](../README.MD)), and optionally paste them into `docs/iac/ps1-az-scripts/.env` (create from `.env.example`) for manual deployment.

If you used Option B, the console outputs everything you need after creating the secret key (scroll up for the subscription ID).
