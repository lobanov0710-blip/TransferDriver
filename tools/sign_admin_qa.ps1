param(
    [Parameter(Mandatory=$true)][string]$UnsignedApk,
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-fA-F0-9]{64}$')][string]$ExpectedSha256,
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-fA-F0-9]{64}$')][string]$ExpectedCertificateSha256,
    [Parameter(Mandatory=$true)][string]$KeyStore,
    [Parameter(Mandatory=$true)][string]$OutputApk,
    [string]$SdkRoot = "$env:LOCALAPPDATA\Android\Sdk"
)
$ErrorActionPreference = 'Stop'
$signer = Join-Path $SdkRoot 'build-tools\37.0.0\apksigner.bat'
$aapt = Join-Path $SdkRoot 'build-tools\37.0.0\aapt.exe'
$zipalign = Join-Path $SdkRoot 'build-tools\37.0.0\zipalign.exe'
foreach ($file in @($UnsignedApk, $KeyStore, $signer, $aapt, $zipalign)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw 'Required file is missing.' }
}
if (Test-Path -LiteralPath $OutputApk) { throw 'Output already exists; choose a new filename.' }
if ((Get-FileHash -LiteralPath $UnsignedApk -Algorithm SHA256).Hash -ne $ExpectedSha256) {
    throw 'Unsigned APK hash does not match the verified build receipt.'
}
$manifest = & $aapt dump xmltree $UnsignedApk AndroidManifest.xml 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $manifest -notmatch 'A: package="ru\.transferservis\.admin\.qa"') {
    throw 'Expected the separate Admin QA package.'
}
if ($manifest -match 'android:(debuggable|testOnly|allowBackup|usesCleartextTraffic)\([^\r\n]+\)=\(type 0x12\)0xffffffff' -or $manifest -match 'sharedUserId') {
    throw 'APK security flags are invalid.'
}
& $zipalign -c -P 16 4 $UnsignedApk
if ($LASTEXITCODE -ne 0) { throw 'Unsigned APK alignment check failed.' }
# No password arguments/environment/files: apksigner prompts in the private terminal.
& $signer sign --ks $KeyStore --ks-key-alias transfer-admin-qa --out $OutputApk $UnsignedApk
if ($LASTEXITCODE -ne 0) {
    Remove-Item -LiteralPath $OutputApk -ErrorAction SilentlyContinue
    throw 'Local signing failed; no installable output was accepted.'
}
$verification = & $signer verify --print-certs $OutputApk 2>&1 | Out-String
$verified = $LASTEXITCODE -eq 0
$certificates = [regex]::Matches($verification, 'certificate SHA-256 digest: ([a-fA-F0-9]{64})')
if (-not $verified -or $certificates.Count -ne 1 -or $certificates[0].Groups[1].Value -ne $ExpectedCertificateSha256) {
    Remove-Item -LiteralPath $OutputApk -ErrorAction SilentlyContinue
    throw 'Signed APK did not match the pinned QA certificate.'
}
Write-Host 'QA_SIGNING=PASS; keep this same private key for all future QA updates.'
Get-FileHash -LiteralPath $OutputApk -Algorithm SHA256
