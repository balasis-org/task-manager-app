# 01 — Create App Registration (One-Time)

> Run once with an Owner/Global Admin account.

## Option A: Azure Portal

1. Go to Azure portal → search for "App registrations" → select **App registrations** under Services → **New registration**
2. Name: e.g. `taskmanager-cicd`
3. Supported account types: **Single tenant** (important — avoids cross-tenant security issues)
4. Click **Register** (skip the redirect URI)
5. On the overview page, note the **Application (client) ID** and **Directory (tenant) ID**
6. Go to **Certificates & secrets → + New client secret**
7. Copy the secret value immediately (you won't see it again)


## Option B: Azure CLI

> **Important:** The Azure Portal auto-creates a Service Principal when you
> register an app. The CLI only creates the App Registration (the definition),
> so you must manually create the Service Principal (the actual identity that
> authenticates and receives role assignments) with `az ad sp create`.

```bash
# login with your Owner account
az login

# create the app registration(default is single-tenant)
az ad app create --display-name taskmanager-cicd

# note the appId from output (this is the Client ID)

# create the service principal (Enterprise Application)
# Portal does this automatically — CLI does NOT
az ad sp create --id <APP_ID>

# create a secret
# (copy-paste the APP_ID from the console)
# (you may also name it using --display-name <name>)
az ad app credential reset --id <APP_ID>
```

## What to store

After copying the secret value, save it somewhere secure. For now you can paste it into
the `.env` file at `docs/iac/ps1-az-scripts/.env` (create one from the `.env.example` —
everything else can be found on the app registration's overview page).

If you used option B, the console outputs everything you need after creating the secret key
(scroll up for the subscription ID).

## Note

You can reuse the same app registration (and its credentials) for the GitHub Actions CI/CD
setup. If you want stricter isolation, create a separate one and scope its privileges to
the resource group level, but this adds setup effort for minimal benefit unless you're
concerned about credential leakage from the GitHub side.
