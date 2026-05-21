param(
    [string]$Container = "employee-manager-postgres",
    [string]$Database = $env:DB_NAME,
    [string]$Username = $env:DB_USERNAME,
    [string]$OutputDir = "var/backups"
)

if ([string]::IsNullOrWhiteSpace($Database)) { $Database = "employee_manager" }
if ([string]::IsNullOrWhiteSpace($Username)) { $Username = "app" }

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$target = Join-Path $OutputDir "$Database-$stamp.dump"

docker exec $Container pg_dump -U $Username -d $Database -Fc | Set-Content -Encoding Byte -Path $target
Write-Output "Backup written to $target"
