$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$jdkHome = Join-Path $env:LOCALAPPDATA 'Programs\Temurin17\jdk-17.0.20.1+1'
$java = Join-Path $jdkHome 'bin\java.exe'
$javac = Join-Path $jdkHome 'bin\javac.exe'
$visualVm = 'C:\ProgramData\chocolatey\lib\VisualVM\tools\visualvm_216\bin\visualvm.exe'
$outDirectory = Join-Path $projectRoot 'out'
$outputLog = Join-Path $projectRoot 'lab-output.log'
$errorLog = Join-Path $projectRoot 'lab-error.log'

if (-not (Test-Path $java) -or -not (Test-Path $javac)) {
    throw "JDK 17 not found: $jdkHome"
}
if (-not (Test-Path $visualVm)) {
    throw "VisualVM not found: $visualVm"
}

New-Item -ItemType Directory -Force $outDirectory | Out-Null
& $javac -d $outDirectory (Join-Path $projectRoot 'src\Main.java')
if ($LASTEXITCODE -ne 0) {
    throw 'Compilation failed.'
}

$javaProcess = Start-Process -FilePath $java `
    -ArgumentList '-Xms128m', '-Xmx512m', '-XX:+UseG1GC', '-cp', $outDirectory, 'Main' `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput $outputLog `
    -RedirectStandardError $errorLog `
    -PassThru

Start-Process -FilePath $visualVm `
    -ArgumentList '--jdkhome', $jdkHome, '--console', 'suppress'

Write-Host "Java application started. PID: $($javaProcess.Id)"
Write-Host 'VisualVM is opening. Double-click Main in the Local section.'
Write-Host "Output log: $outputLog"
