param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Resolve-Path -LiteralPath $Path).Path
}

function Resolve-TargetPath {
    param([Parameter(Mandatory = $true)][string]$Path)
    $parent = Split-Path -Parent $Path
    $leaf = Split-Path -Leaf $Path
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    return [System.IO.Path]::GetFullPath((Join-Path (Resolve-ExistingPath $parent) $leaf))
}

function Assert-InToolsDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    $resolvedPath = Resolve-TargetPath $Path
    $resolvedTools = Resolve-ExistingPath $ToolsRoot
    $toolsPrefix = $resolvedTools.TrimEnd([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar

    if ($resolvedPath -ne $resolvedTools -and -not $resolvedPath.StartsWith($toolsPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside project .tools directory: $resolvedPath"
    }

    return $resolvedPath
}

function Remove-SafeDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    if (Test-Path -LiteralPath $Path) {
        $safePath = Assert-InToolsDirectory -Path $Path -ToolsRoot $ToolsRoot
        Remove-Item -LiteralPath $safePath -Recurse -Force
    }
}

function Move-SafeItem {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    $safeSource = Assert-InToolsDirectory -Path $Source -ToolsRoot $ToolsRoot
    $safeDestination = Assert-InToolsDirectory -Path $Destination -ToolsRoot $ToolsRoot
    Move-Item -LiteralPath $safeSource -Destination $safeDestination -Force
}

function Download-File {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Destination
    )

    if (-not (Test-Path -LiteralPath $Destination)) {
        Invoke-WebRequest -Uri $Url -OutFile $Destination
    }
}

function Test-ValidJavaHome {
    param([string]$JavaHome)

    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        return $false
    }

    return Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe")
}

function Expand-ZipIntoDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$ZipPath,
        [Parameter(Mandatory = $true)][string]$Destination,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    Remove-SafeDirectory -Path $Destination -ToolsRoot $ToolsRoot
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Expand-Archive -LiteralPath $ZipPath -DestinationPath $Destination -Force
}

$ProjectRoot = Resolve-TargetPath $ProjectRoot
$ToolsRoot = Join-Path $ProjectRoot ".tools"
$GradleRoot = Join-Path $ToolsRoot "gradle-8.7"
$AndroidSdkRoot = Join-Path $ToolsRoot "android-sdk"
$CmdlineToolsRoot = Join-Path $AndroidSdkRoot "cmdline-tools"
$CmdlineToolsLatest = Join-Path $CmdlineToolsRoot "latest"
$JdkRoot = Join-Path $ToolsRoot "jdk-17"

New-Item -ItemType Directory -Force -Path $ToolsRoot, $GradleRoot, $AndroidSdkRoot, $CmdlineToolsLatest, $JdkRoot | Out-Null

$gradleBat = Join-Path $GradleRoot "bin\gradle.bat"
if (-not (Test-Path -LiteralPath $gradleBat)) {
    $gradleZip = Join-Path $ToolsRoot "gradle-8.7-bin.zip"
    $gradleExtract = Join-Path $ToolsRoot "gradle-extract"
    Download-File -Url "https://services.gradle.org/distributions/gradle-8.7-bin.zip" -Destination $gradleZip
    Expand-ZipIntoDirectory -ZipPath $gradleZip -Destination $gradleExtract -ToolsRoot $ToolsRoot
    $extractedGradle = Join-Path $gradleExtract "gradle-8.7"
    Remove-SafeDirectory -Path $GradleRoot -ToolsRoot $ToolsRoot
    Move-SafeItem -Source $extractedGradle -Destination $GradleRoot -ToolsRoot $ToolsRoot
    Remove-SafeDirectory -Path $gradleExtract -ToolsRoot $ToolsRoot
}

$sdkManager = Join-Path $CmdlineToolsLatest "bin\sdkmanager.bat"
if (-not (Test-Path -LiteralPath $sdkManager)) {
    $cmdlineZip = Join-Path $ToolsRoot "commandlinetools-win-11076708_latest.zip"
    $cmdlineExtract = Join-Path $ToolsRoot "cmdline-tools-extract"
    Download-File -Url "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -Destination $cmdlineZip
    Expand-ZipIntoDirectory -ZipPath $cmdlineZip -Destination $cmdlineExtract -ToolsRoot $ToolsRoot
    $extractedCmdlineTools = Join-Path $cmdlineExtract "cmdline-tools"
    Remove-SafeDirectory -Path $CmdlineToolsLatest -ToolsRoot $ToolsRoot
    Move-SafeItem -Source $extractedCmdlineTools -Destination $CmdlineToolsLatest -ToolsRoot $ToolsRoot
    Remove-SafeDirectory -Path $cmdlineExtract -ToolsRoot $ToolsRoot
}

$localJava = Join-Path $JdkRoot "bin\java.exe"
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
$hasInvalidJavaHome = -not [string]::IsNullOrWhiteSpace($env:JAVA_HOME) -and -not (Test-ValidJavaHome -JavaHome $env:JAVA_HOME)

if ((-not $javaCommand -or $hasInvalidJavaHome) -and -not (Test-Path -LiteralPath $localJava)) {
    $jdkZip = Join-Path $ToolsRoot "jdk-17.zip"
    $jdkExtract = Join-Path $ToolsRoot "jdk-17-extract"
    Download-File -Url "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" -Destination $jdkZip
    Expand-ZipIntoDirectory -ZipPath $jdkZip -Destination $jdkExtract -ToolsRoot $ToolsRoot
    $extractedJdk = Get-ChildItem -LiteralPath $jdkExtract -Directory | Select-Object -First 1
    if (-not $extractedJdk) {
        throw "Unable to locate extracted JDK directory."
    }
    Remove-SafeDirectory -Path $JdkRoot -ToolsRoot $ToolsRoot
    Move-SafeItem -Source $extractedJdk.FullName -Destination $JdkRoot -ToolsRoot $ToolsRoot
    Remove-SafeDirectory -Path $jdkExtract -ToolsRoot $ToolsRoot
}

if (Test-Path -LiteralPath $localJava) {
    $env:JAVA_HOME = $JdkRoot
    $env:Path = (Join-Path $JdkRoot "bin") + [System.IO.Path]::PathSeparator + $env:Path
}

$env:ANDROID_HOME = $AndroidSdkRoot
$env:ANDROID_SDK_ROOT = $AndroidSdkRoot
$pathCheckOverride = "-Dandroid.overridePathCheck=true"
if ([string]::IsNullOrWhiteSpace($env:GRADLE_OPTS)) {
    $env:GRADLE_OPTS = $pathCheckOverride
}
elseif (-not $env:GRADLE_OPTS.Contains($pathCheckOverride)) {
    $env:GRADLE_OPTS = "$pathCheckOverride $env:GRADLE_OPTS"
}
Set-Item -Path "Env:ORG_GRADLE_PROJECT_android.overridePathCheck" -Value "true"

1..100 | ForEach-Object { "y" } | & $sdkManager --sdk_root=$AndroidSdkRoot --licenses | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Android SDK license acceptance failed with exit code $LASTEXITCODE."
}

& $sdkManager --sdk_root=$AndroidSdkRoot "platform-tools" "platforms;android-35" "build-tools;35.0.0"
if ($LASTEXITCODE -ne 0) {
    throw "Android SDK package installation failed with exit code $LASTEXITCODE."
}

$chronaAndroidRoot = Join-Path $ProjectRoot "ChronaAndroid"
New-Item -ItemType Directory -Force -Path $chronaAndroidRoot | Out-Null
$sdkDir = $AndroidSdkRoot.Replace("\", "/")
$localPropertiesPath = Join-Path $chronaAndroidRoot "local.properties"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($localPropertiesPath, "sdk.dir=$sdkDir`r`n", $utf8NoBom)

Write-Host "Gradle: $GradleRoot"
Write-Host "Android SDK: $AndroidSdkRoot"
Write-Host "Setup complete."
