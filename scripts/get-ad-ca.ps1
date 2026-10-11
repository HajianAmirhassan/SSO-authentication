<#
.SYNOPSIS
  گواهی CA (و intermediate) سرور LDAPS دامنه را می‌گیرد و به‌صورت PEM در sso/certs ذخیره می‌کند
  تا Keycloak به اتصال ldaps:// اعتماد کند.
.EXAMPLE
  .\get-ad-ca.ps1 -Server headdcsrv1.palaz.local
#>
param(
    [string]$Server = "headdcsrv1.palaz.local",
    [int]$Port = 636,
    [string]$OutFile = (Join-Path $PSScriptRoot "..\certs\ad-ca.pem")
)

$ErrorActionPreference = "Stop"

$tcp = New-Object System.Net.Sockets.TcpClient($Server, $Port)
try {
    # فقط برای خواندن زنجیره گواهی؛ اعتبارسنجی در Keycloak انجام می‌شود
    $callback = [System.Net.Security.RemoteCertificateValidationCallback] { param($s, $c, $ch, $e) $true }
    $ssl = New-Object System.Net.Security.SslStream($tcp.GetStream(), $false, $callback)
    $ssl.AuthenticateAsClient($Server)
    $leaf = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate)
}
finally {
    $tcp.Close()
}

$chain = New-Object System.Security.Cryptography.X509Certificates.X509Chain
$chain.ChainPolicy.RevocationMode = "NoCheck"
[void]$chain.Build($leaf)

$elements = @($chain.ChainElements | ForEach-Object { $_.Certificate })
if ($elements.Count -gt 1) {
    # گواهی خود سرور تمدید می‌شود؛ فقط CAها را نگه می‌داریم
    $elements = $elements | Select-Object -Skip 1
}
else {
    Write-Warning "زنجیره CA پیدا نشد (گواهی self-signed یا CA در این ماشین trusted نیست). گواهی خود سرور ذخیره می‌شود."
}

$pem = foreach ($cert in $elements) {
    "-----BEGIN CERTIFICATE-----"
    [Convert]::ToBase64String($cert.RawData, "InsertLineBreaks")
    "-----END CERTIFICATE-----"
}

New-Item -ItemType Directory -Force (Split-Path $OutFile) | Out-Null
[IO.File]::WriteAllText($OutFile, (($pem -join "`n") + "`n"))

Write-Host "Server cert : $($leaf.Subject)  (expires $($leaf.NotAfter))"
foreach ($cert in $elements) { Write-Host "Saved CA    : $($cert.Subject)" }
Write-Host "Output      : $((Resolve-Path $OutFile).Path)"
