<#
.SYNOPSIS
  Fresh-Windows setup script for the helpdesk project.
  Installs all system-level tools needed to build, run, and develop this repo.

.DESCRIPTION
  Installs: JDK 21, Node.js LTS, Git, GitHub CLI, Docker Desktop, VS Code,
  plus the VS Code extensions actually used by this project.
  Drops the full system inventory (currently installed apps) to setup-inventory.txt
  for reference before reinstalling.

.EXAMPLE
  .\setup.ps1
#>

$ErrorActionPreference = 'Stop'
$ProgressPreference    = 'SilentlyContinue'

function Section($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }
function Step($msg)      { Write-Host "  $msg" -ForegroundColor DarkGray }

# 0. Snapshot current inventory (before reinstall, for reference)
Section "0. INVENTORY SNAPSHOT"
$inv = "$PSScriptRoot\setup-inventory.txt"
Write-Host "Saving inventory of currently installed apps to: $inv"
winget export -o "$inv" 2>$null | Out-Null
Get-AppxPackage 2>$null | Select-Object Name, PackageFullName |
  Sort-Object Name | Format-Table -AutoSize |
  Out-File "$PSScriptRoot\setup-appx.txt" -Encoding utf8
"Existing inventory exported to setup-inventory.txt and setup-appx.txt."

# 1. System tools (winget)
Section "1. SYSTEM TOOLS"
$tools = @(
    'Oracle.JDK.21'
    'OpenJS.NodeJS.LTS'
    'Git.Git'
    'GitHub.cli'
    'Microsoft.VisualStudioCode'
    'Docker.DockerDesktop'
)
foreach ($t in $tools) {
    Step "Installing $t ..."
    winget install --id "$t" --accept-package-agreements --accept-source-agreements --silent
}

# 2. VS Code extensions (only the ones this project uses)
Section "2. VS CODE EXTENSIONS"
$extensions = @(
    # Java backend
    'redhat.java',
    'vscjava.vscode-java-pack',
    'vscjava.vscode-java-debug',
    'vscjava.vscode-java-dependency',
    'vscjava.vscode-java-test',
    'vscjava.vscode-maven',
    'ms-vscode.cpptools',
    'ms-vscode.cpptools-extension-pack'
    # Git
    'eamodio.gitlens',
    'mhutchie.git-graph'
    # AI assistants
    'anthropic.claude-code',
    'openai.chatgpt'
)
foreach ($ext in $extensions) {
    Step "Installing extension $ext ..."
    code --install-extension $ext --force
}

# 3. Bootstrap the project dependencies
Section "3. PROJECT DEPENDENCIES"
Step "Maven wrapper (backend) ..."
Push-Location "$PSScriptRoot\backend\api"
    if (Test-Path .\mvnw.cmd) {
        .\mvnw.cmd clean install -DskipTests
    } else { Write-Host "  [warn] mvnw.cmd not found; skipping Maven bootstrap." }
Pop-Location

Step "npm install (frontend) ..."
Push-Location "$PSScriptRoot\frontend"
    if (Test-Path .\package.json) {
        npm install
    } else { Write-Host "  [warn] package.json not found; skipping npm install." }
Pop-Location

# 4. Infrastructure (docker-compose)
Section "4. INFRASTRUCTURE (docker compose)"
Step "Starting RabbitMQ + MailHog ..."
Push-Location "$PSScriptRoot"
    docker compose up -d
Pop-Location

# 5. Done — show what to run next
Section "5. READY TO DEV"
@(
    "Backend:  cd backend\api ; .\mvnw.cmd spring-boot:run"
    "Frontend: cd frontend   ; npm run dev"
    "DB:       Supabase cloud (configured in backend\api\.env)"
    "Apps:"
    "    Backend API   -> http://localhost:8080"
    "    Frontend UI   -> http://localhost:5173"
    "    RabbitMQ mgmt -> http://localhost:15672  (helpdesk / helpdesk)"
    "    MailHog UI    -> http://localhost:8025"
) | ForEach-Object { Write-Host "  $_" -ForegroundColor Green }

Write-Host "`nSetup complete. Restart your terminal before launching the apps above." -ForegroundColor Yellow
Write-Host "LICENSE NOTE: ensure Oracle is enabled to use JDK 21 with your Oracle account." -ForegroundColor DarkGray