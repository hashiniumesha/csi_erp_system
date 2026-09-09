<#
.SYNOPSIS
  Builds Ceylon Sweets Island ERP into a real double-click desktop app
  (no terminal windows) instead of running it via `mvnw spring-boot:run` /
  `mvnw javafx:run` in two separate terminals.

.DESCRIPTION
  Produces frontend/target/dist/ containing:
    "Ceylon Sweets Island ERP"/Ceylon Sweets Island ERP.exe   <- double-click this
    backend/erp-backend.jar

  The .exe bundles its own full Java runtime, so the machine it runs on
  does NOT need Java installed. When you double-click it, it automatically
  starts the backend (invisibly, no console window) if it isn't already
  running, waits for it to be ready, then shows the login screen - see
  BackendLauncher.java / MainApp.java in the frontend source for how.

  What this does NOT solve: MySQL. The machine running this still needs
  MySQL installed, the MySQL80 service running, and the csi_erp_db
  database/schema/seed data already loaded - same as running it the
  Maven way. If that's missing, the app now shows a clear in-app message
  explaining that, instead of a raw stack trace.

.PARAMETER Configuration
  Not used yet - reserved in case Debug/Release variants are ever needed.

.EXAMPLE
  .\build-desktop-app.ps1
  Then open frontend\target\dist\ and double-click the .exe inside
  "Ceylon Sweets Island ERP" to try it.
#>

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"

# jpackage needs a FULL JDK/JRE image (not the minimal one it would
# auto-generate) so the resulting runtime/bin/javaw.exe actually exists -
# that javaw.exe is what's reused to start the backend jar invisibly.
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
    if ($javaExe) { $javaHome = Split-Path (Split-Path $javaExe -Parent) -Parent }
}
if (-not $javaHome -or -not (Test-Path $javaHome)) {
    throw "Couldn't find a JDK. Set JAVA_HOME or make sure 'java' is on PATH."
}
Write-Host "Using JDK at: $javaHome"

Write-Host "`n=== 1/4  Building backend (Spring Boot fat jar) ===" -ForegroundColor Cyan
Push-Location $backend
& .\mvnw.cmd -q package "-DskipTests"
if ($LASTEXITCODE -ne 0) { throw "Backend build failed." }
Pop-Location

$backendJar = Get-ChildItem (Join-Path $backend "target") -Filter "erp-backend-*.jar" | Select-Object -First 1
if (-not $backendJar) { throw "Backend jar not found after build." }

Write-Host "`n=== 2/4  Building frontend jar + gathering its dependencies ===" -ForegroundColor Cyan
Push-Location $frontend
& .\mvnw.cmd -q package "-DskipTests"
if ($LASTEXITCODE -ne 0) { throw "Frontend build failed." }
& .\mvnw.cmd -q dependency:copy-dependencies "-DoutputDirectory=target/libs" "-DincludeScope=runtime"
if ($LASTEXITCODE -ne 0) { throw "Gathering frontend dependencies failed." }
Pop-Location

$jpackageInput = Join-Path $frontend "target\jpackage-input"
Remove-Item $jpackageInput -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $jpackageInput | Out-Null
Copy-Item (Join-Path $frontend "target\erp-frontend-*.jar") $jpackageInput
Copy-Item (Join-Path $frontend "target\libs\*.jar") $jpackageInput

$dist = Join-Path $frontend "target\dist"
Remove-Item $dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $dist | Out-Null

Write-Host "`n=== 3/4  Packaging the desktop app (jpackage) ===" -ForegroundColor Cyan
$frontendJarName = (Get-ChildItem $jpackageInput -Filter "erp-frontend-*.jar" | Select-Object -First 1).Name
& jpackage `
    --type app-image `
    --input $jpackageInput `
    --dest $dist `
    --name "Ceylon Sweets Island ERP" `
    --main-jar $frontendJarName `
    --main-class com.csi.erpfrontend.Launcher `
    --icon (Join-Path $root "csi-logo.ico") `
    --vendor "Ceylon Sweets Island" `
    --app-version "1.0.0" `
    --runtime-image $javaHome
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

Write-Host "`n=== 4/4  Bundling the backend jar alongside it ===" -ForegroundColor Cyan
$distBackend = Join-Path $dist "backend"
New-Item -ItemType Directory -Path $distBackend -Force | Out-Null
Copy-Item $backendJar.FullName (Join-Path $distBackend "erp-backend.jar") -Force

Write-Host "`nDone. Double-click:" -ForegroundColor Green
Write-Host "  $dist\Ceylon Sweets Island ERP\Ceylon Sweets Island ERP.exe`n"
Write-Host "Make sure MySQL (MySQL80 service) is running and csi_erp_db is set up before you do." -ForegroundColor Yellow
