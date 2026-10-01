$labProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
    $_.Name -eq 'java.exe' -and $_.CommandLine -match 'out.*Main'
}

foreach ($process in $labProcesses) {
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped lab JVM PID $($process.ProcessId)"
}

$visualVmProcesses = Get-Process visualvm -ErrorAction SilentlyContinue
if ($visualVmProcesses) {
    $visualVmProcesses | Stop-Process -Force
    Write-Host "Stopped VisualVM PID(s): $($visualVmProcesses.Id -join ', ')"
} else {
    Write-Host 'VisualVM is not running.'
}
