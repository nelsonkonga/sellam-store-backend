# Script pour charger les variables .env et lancer Spring Boot

# 1. Charger le fichier .env
Write-Host "Chargement des variables d'environnement depuis .env..." -ForegroundColor Cyan
$envFile = Get-Content .env | Where-Object { $_ -match '^\w+=' }

foreach ($line in $envFile) {
    $name, $value = $line -split '=', 2
    if ($name -and $value) {
        # Trim les espaces
        $name = $name.Trim()
        $value = $value.Trim()
        
        # Set pour le processus courant
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "OK: $name" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Variables chargees! Lancement de Spring Boot..." -ForegroundColor Cyan
Write-Host ""

# 2. Lancer Maven avec les variables chargées
& .\mvnw.cmd spring-boot:run
