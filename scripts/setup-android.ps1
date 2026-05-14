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

function Remove-SafeItem {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    if (Test-Path -LiteralPath $Path) {
        $safePath = Assert-InToolsDirectory -Path $Path -ToolsRoot $ToolsRoot
        Remove-Item -LiteralPath $safePath -Force
    }
}

function Download-File {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$Destination,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    if (-not (Test-Path -LiteralPath $Destination)) {
        $safeDestination = Assert-InToolsDirectory -Path $Destination -ToolsRoot $ToolsRoot
        $tempDestination = "$safeDestination.download"
        Remove-SafeItem -Path $tempDestination -ToolsRoot $ToolsRoot
        Invoke-WebRequest -Uri $Url -OutFile $tempDestination
        Move-SafeItem -Source $tempDestination -Destination $safeDestination -ToolsRoot $ToolsRoot
    }
}

function Add-PathEntryIfMissing {
    param([Parameter(Mandatory = $true)][string]$PathEntry)

    $pathEntries = $env:Path -split [System.IO.Path]::PathSeparator
    $alreadyPresent = $pathEntries | Where-Object { $_ -eq $PathEntry } | Select-Object -First 1

    if (-not $alreadyPresent) {
        $env:Path = $PathEntry + [System.IO.Path]::PathSeparator + $env:Path
    }
}

function Get-JavaMajorVersion {
    param([Parameter(Mandatory = $true)][string]$JavaExe)

    if (-not (Test-Path -LiteralPath $JavaExe)) {
        return $null
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $versionOutput = & $JavaExe -version 2>&1
        $javaExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($javaExitCode -ne 0) {
        return $null
    }

    $versionText = $versionOutput -join "`n"
    $match = [regex]::Match($versionText, 'version "([^"]+)"')
    if (-not $match.Success) {
        return $null
    }

    $version = $match.Groups[1].Value
    if ($version.StartsWith("1.")) {
        $legacyParts = $version.Split(".")
        $legacyMajor = 0
        if ($legacyParts.Length -lt 2 -or -not [int]::TryParse($legacyParts[1], [ref]$legacyMajor)) {
            return $null
        }

        return $legacyMajor
    }

    $majorText = $version.Split(".")[0]
    $majorVersion = 0
    if (-not [int]::TryParse($majorText, [ref]$majorVersion)) {
        return $null
    }

    return $majorVersion
}

function Test-CompatibleJavaHome {
    param([Parameter(Mandatory = $true)][string]$JavaHome)

    $JavaExe = Join-Path $JavaHome "bin\java.exe"
    $JavacExe = Join-Path $JavaHome "bin\javac.exe"
    if (-not (Test-Path -LiteralPath $JavaExe) -or -not (Test-Path -LiteralPath $JavacExe)) {
        return $false
    }

    $majorVersion = Get-JavaMajorVersion -JavaExe $JavaExe
    return $null -ne $majorVersion -and $majorVersion -ge 17
}

function Get-CompatibleJavaHomeFromExecutable {
    param([Parameter(Mandatory = $true)][string]$JavaExe)

    if (-not (Test-Path -LiteralPath $JavaExe)) {
        return $null
    }

    $javaParent = Split-Path -Parent $JavaExe
    if ((Split-Path -Leaf $javaParent) -ne "bin") {
        return $null
    }

    $candidateHome = Split-Path -Parent $javaParent
    if (Test-CompatibleJavaHome -JavaHome $candidateHome) {
        return $candidateHome
    }

    return $null
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

function Expand-DownloadedZipIntoDirectory {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$ZipPath,
        [Parameter(Mandatory = $true)][string]$Destination,
        [Parameter(Mandatory = $true)][string]$ToolsRoot
    )

    Download-File -Url $Url -Destination $ZipPath -ToolsRoot $ToolsRoot

    try {
        Expand-ZipIntoDirectory -ZipPath $ZipPath -Destination $Destination -ToolsRoot $ToolsRoot
    }
    catch {
        Remove-SafeItem -Path $ZipPath -ToolsRoot $ToolsRoot
        Download-File -Url $Url -Destination $ZipPath -ToolsRoot $ToolsRoot
        Expand-ZipIntoDirectory -ZipPath $ZipPath -Destination $Destination -ToolsRoot $ToolsRoot
    }
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
    Expand-DownloadedZipIntoDirectory -Url "https://services.gradle.org/distributions/gradle-8.7-bin.zip" -ZipPath $gradleZip -Destination $gradleExtract -ToolsRoot $ToolsRoot
    $extractedGradle = Join-Path $gradleExtract "gradle-8.7"
    Remove-SafeDirectory -Path $GradleRoot -ToolsRoot $ToolsRoot
    Move-SafeItem -Source $extractedGradle -Destination $GradleRoot -ToolsRoot $ToolsRoot
    Remove-SafeDirectory -Path $gradleExtract -ToolsRoot $ToolsRoot
}

$sdkManager = Join-Path $CmdlineToolsLatest "bin\sdkmanager.bat"
if (-not (Test-Path -LiteralPath $sdkManager)) {
    $cmdlineZip = Join-Path $ToolsRoot "commandlinetools-win-11076708_latest.zip"
    $cmdlineExtract = Join-Path $ToolsRoot "cmdline-tools-extract"
    Expand-DownloadedZipIntoDirectory -Url "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -ZipPath $cmdlineZip -Destination $cmdlineExtract -ToolsRoot $ToolsRoot
    $extractedCmdlineTools = Join-Path $cmdlineExtract "cmdline-tools"
    Remove-SafeDirectory -Path $CmdlineToolsLatest -ToolsRoot $ToolsRoot
    Move-SafeItem -Source $extractedCmdlineTools -Destination $CmdlineToolsLatest -ToolsRoot $ToolsRoot
    Remove-SafeDirectory -Path $cmdlineExtract -ToolsRoot $ToolsRoot
}

$compatibleJavaHome = $null

if (Test-CompatibleJavaHome -JavaHome $JdkRoot) {
    $compatibleJavaHome = $JdkRoot
}

if (-not $compatibleJavaHome -and -not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    if (Test-CompatibleJavaHome -JavaHome $env:JAVA_HOME) {
        $compatibleJavaHome = $env:JAVA_HOME
    }
}

if (-not $compatibleJavaHome) {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $compatibleJavaHome = Get-CompatibleJavaHomeFromExecutable -JavaExe $javaCommand.Source
    }
}

if (-not $compatibleJavaHome) {
    $jdkZip = Join-Path $ToolsRoot "jdk-17.zip"
    $jdkExtract = Join-Path $ToolsRoot "jdk-17-extract"
    Expand-DownloadedZipIntoDirectory -Url "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" -ZipPath $jdkZip -Destination $jdkExtract -ToolsRoot $ToolsRoot
    $extractedJdk = Get-ChildItem -LiteralPath $jdkExtract -Directory | Select-Object -First 1
    if (-not $extractedJdk) {
        throw "Unable to locate extracted JDK directory."
    }
    Remove-SafeDirectory -Path $JdkRoot -ToolsRoot $ToolsRoot
    Move-SafeItem -Source $extractedJdk.FullName -Destination $JdkRoot -ToolsRoot $ToolsRoot
    Remove-SafeDirectory -Path $jdkExtract -ToolsRoot $ToolsRoot
    $compatibleJavaHome = $JdkRoot
}

if (Test-CompatibleJavaHome -JavaHome $compatibleJavaHome) {
    $env:JAVA_HOME = $compatibleJavaHome
    Add-PathEntryIfMissing -PathEntry (Join-Path $compatibleJavaHome "bin")
}
else {
    throw "Unable to locate or install a Java 17+ JDK."
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
Set-Item -Path "Env:ORG_GRADLE_PROJECT_android.useAndroidX" -Value "true"

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
