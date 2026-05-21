param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$Container = "employee-manager-postgres",
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME
)

if ([string]::IsNullOrWhiteSpace($Database)) { $Database = "employee_manager" }
if ([string]::IsNullOrWhiteSpace($Username)) { $Username = "app" }

Get-Content -Encoding Byte -Path $BackupFile | docker exec -i $Container pg_restore -U $Username -d $Database --clean --if-exists
Write-Output "Restore completed from $BackupFile"
