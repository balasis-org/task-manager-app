# 01 — Create App Registration (One-Time)

> Run once with an Owner/Global Admin account.
> You may want to repeat this md guide twice in case you want different app registrations for your manual
> use and GitHub ci-cd ; however I would recommend to use the same credentials for simplicity(not much of a benefit to split)

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
# login with your Owner account (choose your subscription)
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
# Note: we could also use a certificate;(personal preference of avoiding self-signed ones
#       and we want to avoid extra charges for CA...so we stick with secrets for now).
az ad app credential reset --id <APP_ID>
```

## What to store

After copying the secret value, save it somewhere secure. For now you can paste it into
the `.env` file at `docs/iac/ps1-az-scripts/.env` (create one from the `.env.example` —
everything else can be found on the app registration's overview page). Moreover, if you
want to also use the same got github cicd actions perhaps now its a good time also to update your github
secrets.
Git CI-CDs expect :
-AZURE_CLIENT_ID (refers to app id)
-AZURE_CLIENT_SECRET (refers to password if you used console / it's the secret value if used azure portal)
-AZURE_SUBSCRIPTION_ID
-AZURE_TENANT_ID

If you used option B, the console outputs everything you need after creating the secret key
(scroll up for the subscription ID).
