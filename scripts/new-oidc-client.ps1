<#
.SYNOPSIS
  ساخت یا به‌روزرسانی یک Client از نوع OIDC (confidential، Authorization Code + PKCE) در Realm palaz
  برای یک اپ ASP.NET Core که از AddOpenIdConnect استفاده می‌کند.
.DESCRIPTION
  - Redirect URI: <BaseUrl>/signin-oidc ، Post logout: <BaseUrl>/*
  - با ادمین موقت داخل .env به Admin API وصل می‌شود
  - اگر -UserSecretsProject داده شود، Client Secret مستقیم در dotnet user-secrets آن پروژه
    (کلید Sso:ClientSecret) ذخیره می‌شود و چاپ نمی‌شود
.EXAMPLE
  .\new-oidc-client.ps1 -ClientId crm-supervisor-dashboard -Name "CRM Supervisor Dashboard" `
      -BaseUrls https://localhost:5003,http://localhost:6003 `
      -UserSecretsProject D:\src\crm\Presentation\PalazCrm.SupervisorDashboard
#>
param(
    [Parameter(Mandatory)] [string]$ClientId,
    [Parameter(Mandatory)] [string[]]$BaseUrls,
    [string]$Name,
    [string]$KeycloakUrl = "http://localhost:8080",
    [string]$Realm = "palaz",
    [string]$EnvFile,
    [string]$UserSecretsProject
)

$ErrorActionPreference = "Stop"
# در Windows PowerShell 5.1، $PSScriptRoot داخل پیش‌فرض پارامترهای Mandatory خالی است
if (-not $EnvFile) { $EnvFile = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "..\.env" }

$envMap = @{}
Get-Content $EnvFile | Where-Object { $_ -match '^\s*([A-Z_]+)=(.*)$' } | ForEach-Object { $envMap[$Matches[1]] = $Matches[2] }

$token = (Invoke-RestMethod -Method Post "$KeycloakUrl/realms/master/protocol/openid-connect/token" -Body @{
        grant_type = "password"; client_id = "admin-cli"
        username = $envMap.KC_BOOTSTRAP_ADMIN_USERNAME; password = $envMap.KC_BOOTSTRAP_ADMIN_PASSWORD
    }).access_token
$headers = @{ Authorization = "Bearer $token" }
$api = "$KeycloakUrl/admin/realms/$Realm"

# با powershell -File، لیست به‌صورت یک رشته با کاما می‌رسد
$BaseUrls = @($BaseUrls | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim().TrimEnd('/') } | Where-Object { $_ })
$client = @{
    clientId                  = $ClientId
    name                      = $(if ($Name) { $Name } else { $ClientId })
    protocol                  = "openid-connect"
    enabled                   = $true
    publicClient              = $false
    clientAuthenticatorType   = "client-secret"
    standardFlowEnabled       = $true
    implicitFlowEnabled       = $false
    directAccessGrantsEnabled = $false
    serviceAccountsEnabled    = $false
    frontchannelLogout        = $true
    rootUrl                   = $BaseUrls[0]
    baseUrl                   = "/"
    redirectUris              = @($BaseUrls | ForEach-Object { "$_/signin-oidc" })
    webOrigins                = @()
    attributes                = @{
        "pkce.code.challenge.method" = "S256"
        "post.logout.redirect.uris"  = (($BaseUrls | ForEach-Object { "$_/*" }) -join "##")
    }
}

$existing = Invoke-RestMethod "$api/clients?clientId=$ClientId" -Headers $headers
if ($existing) {
    $id = $existing[0].id
    $client.id = $id
    # بدون -Name، اسم فعلی Client حفظ شود
    if (-not $Name) { $client.name = $existing[0].name }
    Invoke-RestMethod -Method Put "$api/clients/$id" -Headers $headers -ContentType "application/json; charset=utf-8" `
        -Body ([Text.Encoding]::UTF8.GetBytes(($client | ConvertTo-Json -Depth 5))) | Out-Null
    Write-Host "Updated client '$ClientId'"
}
else {
    Invoke-RestMethod -Method Post "$api/clients" -Headers $headers -ContentType "application/json; charset=utf-8" `
        -Body ([Text.Encoding]::UTF8.GetBytes(($client | ConvertTo-Json -Depth 5))) | Out-Null
    $id = (Invoke-RestMethod "$api/clients?clientId=$ClientId" -Headers $headers)[0].id
    Write-Host "Created client '$ClientId'"
}

Write-Host "Redirect URIs: $($client.redirectUris -join ', ')"

if ($UserSecretsProject) {
    $secret = (Invoke-RestMethod "$api/clients/$id/client-secret" -Headers $headers).value
    dotnet user-secrets set "Sso:ClientSecret" $secret --project $UserSecretsProject | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "dotnet user-secrets failed" }
    Write-Host "Client secret stored in user-secrets of $UserSecretsProject (Sso:ClientSecret)"
}
else {
    Write-Host "Client secret: Keycloak admin > Clients > $ClientId > Credentials"
}
