# 02 — Assign Subscription Roles (One-Time)

> Run once with an Owner account. The CI/CD SP needs two roles at subscription level:
>
> **Contributor** — lets it create and destroy resource groups and all Azure resources.
> **User Access Administrator** — lets it create RBAC role assignments inside the Bicep
> template (Key Vault, Storage, Cognitive Services, ACS, Front Door).
>
> Without User Access Administrator the deployment fails at the RBAC role-assignment
> resources with `AuthorizationFailed`.

## Option A: Azure Portal

1. Go to **Subscriptions → your subscription → Access control (IAM)**
2. Click **+ Add → Add role assignment**
3. Role: **Contributor**
4. Members tab → **User, group, or service principal**
5. Search for your App Registration name (e.g. `MyTeamTasksManual`)
6. Select it → **Review + assign**
7. **Repeat steps 2-6** for the **User Access Administrator** role

## Option B: Azure CLI

```bash
# login with your Owner account
az login

# It will ask you to choose a subscription unless you've set a default.
# The subscription choice here is independent of the role assignment commands below.

# 1. Contributor - create/destroy resources
# (SP_APP_ID is the clientID of the app registration)
# Subscription ID can be copy-pasted from the az login output
az role assignment create \
  --assignee <SP_APP_ID> \
  --role "Contributor" \
  --scope /subscriptions/<SUBSCRIPTION_ID>

# 2. User Access Administrator - create RBAC assignments in Bicep
az role assignment create \
  --assignee <SP_APP_ID> \
  --role "User Access Administrator" \
  --scope /subscriptions/<SUBSCRIPTION_ID>
```

## Why not Owner?

Owner = Contributor + User Access Administrator + more. Assigning the
two roles separately follows least-privilege: the SP can manage resources
and assign specific roles but cannot, for example, manage policy or
blueprints.
