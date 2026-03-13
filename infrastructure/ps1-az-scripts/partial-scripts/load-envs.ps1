param(
  [string]$Path = (Join-Path $PSScriptRoot '..\.env')
)

if (-not (Test-Path $Path)) {
  Write-Error "Env file not found: $Path"
  exit 1
}

Get-Content $Path | ForEach-Object {
  $line = $_.Trim()
  if ($line -eq '' -or $line.StartsWith('#') -or $line.StartsWith(';')) { return }

  $parts = $line -split '=', 2
  if ($parts.Count -ne 2) { 
    Write-Warning ".env contains invalid line: $line"
    return
  }

  $name  = $parts[0].Trim()
  $value = $parts[1].Trim()

  if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
      ($value.StartsWith("'") -and $value.EndsWith("'"))) {
    $value = $value.Substring(1, $value.Length - 2)
  }

  Set-Item -Path "Env:\$name" -Value $value
}

Write-Host "Loaded env from: $Path"
