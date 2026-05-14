param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
$setupScript = Join-Path $ProjectRoot "scripts\setup-android.ps1"
& $setupScript -ProjectRoot $ProjectRoot

$toolsRoot = Join-Path $ProjectRoot ".tools"
$androidSdkRoot = Join-Path $toolsRoot "android-sdk"
$jdkRoot = Join-Path $toolsRoot "jdk-17"
$localJava = Join-Path $jdkRoot "bin\java.exe"

$env:ANDROID_HOME = $androidSdkRoot
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$pathCheckOverride = "-Dandroid.overridePathCheck=true"
if ([string]::IsNullOrWhiteSpace($env:GRADLE_OPTS)) {
    $env:GRADLE_OPTS = $pathCheckOverride
}
elseif (-not $env:GRADLE_OPTS.Contains($pathCheckOverride)) {
    $env:GRADLE_OPTS = "$pathCheckOverride $env:GRADLE_OPTS"
}
Set-Item -Path "Env:ORG_GRADLE_PROJECT_android.overridePathCheck" -Value "true"

if (Test-Path -LiteralPath $localJava) {
    $env:JAVA_HOME = $jdkRoot
    $env:Path = (Join-Path $jdkRoot "bin") + [System.IO.Path]::PathSeparator + $env:Path
}

$gradle = Join-Path $toolsRoot "gradle-8.7\bin\gradle.bat"
$androidProject = Join-Path $ProjectRoot "ChronaAndroid"

Push-Location -LiteralPath $androidProject
try {
    & $gradle ":app:assembleDebug" ":app:testDebugUnitTest"
}
finally {
    Pop-Location
}
