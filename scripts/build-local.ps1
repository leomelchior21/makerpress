$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $project
if (-not $env:JAVA_HOME) {
    $localJdk = Get-ChildItem -LiteralPath (Join-Path $project '.tools/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
}
& ./gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Build or verification failed.' }
New-Item -ItemType Directory -Force -Path dist | Out-Null
$buildMetadata = Get-Content -Raw -LiteralPath app/build/outputs/apk/debug/output-metadata.json | ConvertFrom-Json
$deliveryApk = Join-Path 'dist' ("MakerPress-v{0}-debug.apk" -f $buildMetadata.elements[0].versionName)
Copy-Item -LiteralPath app/build/outputs/apk/debug/app-debug.apk -Destination $deliveryApk
Get-FileHash -Algorithm SHA256 -LiteralPath $deliveryApk
