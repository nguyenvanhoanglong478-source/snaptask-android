# SnapTask - Automated Build & Environment Check Script for Windows
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       SNAPTASK: ANDROID APK BUILD AUTOMATION             " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check for Java
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $javaCmd) {
    Write-Host "[!] Java Development Kit (JDK 17+) is not detected in your PATH." -ForegroundColor Yellow
    Write-Host "    SnapTask can be automatically compiled using GitHub Actions without installing anything locally!" -ForegroundColor Green
    Write-Host "    -> Just push this repo to GitHub, and the workflow in .github/workflows/build_apk.yml will build the APK for you." -ForegroundColor White
    Write-Host ""
    Write-Host "    If you wish to build locally on this PC, install Microsoft OpenJDK 17 or Android Studio." -ForegroundColor Cyan
    Exit 0
} else {
    Write-Host "[✓] Found Java runtime: $($javaCmd.Source)" -ForegroundColor Green
}

# 2. Check Android SDK
$androidHome = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($androidHome)) {
    $androidHome = $env:ANDROID_SDK_ROOT
}

if ([string]::IsNullOrWhiteSpace($androidHome)) {
    Write-Host "[!] ANDROID_HOME or ANDROID_SDK_ROOT is not set." -ForegroundColor Yellow
    Write-Host "    Please ensure Android SDK is installed." -ForegroundColor Yellow
} else {
    Write-Host "[✓] Found Android SDK at: $androidHome" -ForegroundColor Green
}

# 3. Prompt or Run Gradle
Write-Host ""
Write-Host "Running Gradle build for Debug APK..." -ForegroundColor Cyan
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    Write-Host "Run with Gradle installed or use GitHub Actions CI." -ForegroundColor Yellow
}
