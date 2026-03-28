<#
.SYNOPSIS
    Splits the fiasse_benchmark_output monorepo into 24 individual GitHub repositories
    under the Securability-Engineering org.

.DESCRIPTION
    For each leaf directory ({tool}/{language}/{variant}), this script:
    1. Creates a GitHub repo via gh CLI
    2. Copies source files (excluding build artifacts)
    3. Ensures an appropriate .gitignore exists
    4. Initializes git, commits, and pushes

.PARAMETER DryRun
    Preview what would be done without creating repos or pushing code.

.PARAMETER OrgName
    GitHub organization name. Defaults to 'Securability-Engineering'.

.PARAMETER Visibility
    Repository visibility: 'private' or 'public'. Defaults to 'public'.

.PARAMETER SourceRoot
    Root directory of the monorepo. Defaults to the script's directory.

.PARAMETER TempRoot
    Temporary work directory. Defaults to $env:TEMP\split-repos.

.PARAMETER StartAt
    1-based index to resume from (skip repos before this index). Defaults to 1.

.EXAMPLE
    .\split-repos.ps1 -DryRun
    .\split-repos.ps1 -OrgName "Securability-Engineering" -Visibility public
    .\split-repos.ps1 -StartAt 10  # Resume from repo #10
#>

[CmdletBinding(SupportsShouldProcess)]
param(
    [switch]$DryRun,

    [string]$OrgName = "Securability-Engineering",

    [ValidateSet("private", "public")]
    [string]$Visibility = "public",

    [string]$SourceRoot = $PSScriptRoot,

    [string]$TempRoot = (Join-Path $env:TEMP "split-repos"),

    [int]$StartAt = 1
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Repo mappings ────────────────────────────────────────────────────────────
# Each entry: [SourceRelativePath, RepoSuffix, Platform]
# Platform is used to select the right .gitignore template
$Repos = @(
    @{ Source = "claude/aspnet/fiassed";    Repo = "loose-notes_claude_aspnet_fiassed";    Platform = "aspnet" }
    @{ Source = "claude/aspnet/rawdog";     Repo = "loose-notes_claude_aspnet_rawdog";     Platform = "aspnet" }
    @{ Source = "claude/aspnet/securable";  Repo = "loose-notes_claude_aspnet_securable";  Platform = "aspnet" }
    @{ Source = "claude/jsp/fiassed";       Repo = "loose-notes_claude_jsp_fiassed";       Platform = "jsp" }
    @{ Source = "claude/jsp/rawdog";        Repo = "loose-notes_claude_jsp_rawdog";        Platform = "jsp" }
    @{ Source = "claude/jsp/securable";     Repo = "loose-notes_claude_jsp_securable";     Platform = "jsp" }
    @{ Source = "claude/node/fiassed";      Repo = "loose-notes_claude_node_fiassed";      Platform = "node" }
    @{ Source = "claude/node/rawdog";       Repo = "loose-notes_claude_node_rawdog";       Platform = "node" }
    @{ Source = "claude/node/securable";    Repo = "loose-notes_claude_node_securable";    Platform = "node" }
    @{ Source = "copilot/aspnet/fiassed";   Repo = "loose-notes_copilot_aspnet_fiassed";   Platform = "aspnet" }
    @{ Source = "copilot/aspnet/rawdog";    Repo = "loose-notes_copilot_aspnet_rawdog";    Platform = "aspnet" }
    @{ Source = "copilot/aspnet/securable"; Repo = "loose-notes_copilot_aspnet_securable"; Platform = "aspnet" }
    @{ Source = "copilot/jsp/fiassed";      Repo = "loose-notes_copilot_jsp_fiassed";      Platform = "jsp" }
    @{ Source = "copilot/jsp/rawdog";       Repo = "loose-notes_copilot_jsp_rawdog";       Platform = "jsp" }
    @{ Source = "copilot/jsp/securable";    Repo = "loose-notes_copilot_jsp_securable";    Platform = "jsp" }
    @{ Source = "copilot/node/fiassed";     Repo = "loose-notes_copilot_node_fiassed";     Platform = "node" }
    @{ Source = "copilot/node/rawdog";      Repo = "loose-notes_copilot_node_rawdog";      Platform = "node" }
    @{ Source = "copilot/node/securable";   Repo = "loose-notes_copilot_node_securable";   Platform = "node" }
    @{ Source = "opencode/aspnet/rawdog";     Repo = "loose-notes_opencode_aspnet_rawdog";     Platform = "aspnet" }
    @{ Source = "opencode/aspnet/securable";  Repo = "loose-notes_opencode_aspnet_securable";  Platform = "aspnet" }
    @{ Source = "opencode/jsp/rawdog";        Repo = "loose-notes_opencode_jsp_rawdog";        Platform = "jsp" }
    @{ Source = "opencode/jsp/securable";     Repo = "loose-notes_opencode_jsp_securable";     Platform = "jsp" }
    @{ Source = "opencode/node/rawdog";       Repo = "loose-notes_opencode_node_rawdog";       Platform = "node" }
    @{ Source = "opencode/node/securable";    Repo = "loose-notes_opencode_node_securable";    Platform = "node" }
)

# ── Gitignore templates ──────────────────────────────────────────────────────

$NodeGitignore = @"
node_modules/
dist/
.env
*.log
*.db
*.db-shm
*.db-wal
.DS_Store
coverage/
"@

$JspGitignore = @"
target/
*.class
*.jar
*.war
*.ear
*.iml
.idea/
.settings/
.project
.classpath
*.log
.DS_Store
"@

# ── Exclusion patterns for robocopy ──────────────────────────────────────────
$ExcludeDirs = @("bin", "obj", "node_modules", ".git", ".vs", "target", "Debug", "Release")
$ExcludeFiles = @("*.db", "*.db-shm", "*.db-wal")

# ── Helper: generate description from source path ────────────────────────────
function Get-RepoDescription {
    param([string]$SourcePath)
    $parts = $SourcePath -split "/"
    $tool = $parts[0]
    $lang = $parts[1]
    $variant = $parts[2]

    $toolNames = @{
        "claude"   = "Claude Code"
        "copilot"  = "GitHub Copilot CLI"
        "opencode" = "OpenCode CLI"
    }
    $langNames = @{
        "aspnet" = "ASP.NET Core"
        "jsp"    = "Java JSP"
        "node"   = "Node.js/Express"
    }
    $variantNames = @{
        "rawdog"    = "unassisted generation"
        "securable" = "Securable plugin"
        "fiassed"   = "FIASSE-enhanced PRD"
    }

    $t = if ($toolNames.ContainsKey($tool)) { $toolNames[$tool] } else { $tool }
    $l = if ($langNames.ContainsKey($lang)) { $langNames[$lang] } else { $lang }
    $v = if ($variantNames.ContainsKey($variant)) { $variantNames[$variant] } else { $variant }

    return "Loose Notes - $l via $t ($v)"
}

# ── Pre-flight checks ───────────────────────────────────────────────────────
Write-Host "`n=== Split-Repos: Pre-flight Checks ===" -ForegroundColor Cyan

# Check gh CLI
try {
    $ghVersion = gh --version 2>&1 | Select-Object -First 1
    Write-Host "[OK] gh CLI: $ghVersion" -ForegroundColor Green
} catch {
    Write-Error "gh CLI not found. Install from https://cli.github.com/"
    exit 1
}

# Check gh auth
$authStatus = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Not authenticated. Run: gh auth login"
    exit 1
}
Write-Host "[OK] gh authenticated" -ForegroundColor Green

# Check git
try {
    $gitVersion = git --version 2>&1
    Write-Host "[OK] $gitVersion" -ForegroundColor Green
} catch {
    Write-Error "git not found."
    exit 1
}

# Validate source root
if (-not (Test-Path $SourceRoot)) {
    Write-Error "Source root not found: $SourceRoot"
    exit 1
}
Write-Host "[OK] Source root: $SourceRoot" -ForegroundColor Green

# Validate all source directories exist
$missing = @()
foreach ($r in $Repos) {
    $srcPath = Join-Path $SourceRoot $r.Source
    if (-not (Test-Path $srcPath)) {
        $missing += $r.Source
    }
}
if ($missing.Count -gt 0) {
    Write-Warning "Missing source directories:`n  $($missing -join "`n  ")"
    Write-Warning "These repos will be skipped."
}

Write-Host "`n=== Configuration ===" -ForegroundColor Cyan
Write-Host "  Org:        $OrgName"
Write-Host "  Visibility: $Visibility"
Write-Host "  Repos:      $($Repos.Count)"
Write-Host "  DryRun:     $DryRun"
Write-Host "  StartAt:    $StartAt"
Write-Host ""

if ($DryRun) {
    Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Yellow
    Write-Host "║  DRY RUN MODE — No repos will be created or pushed         ║" -ForegroundColor Yellow
    Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Yellow
    Write-Host ""
}

# ── Main loop ────────────────────────────────────────────────────────────────
$total = $Repos.Count
$succeeded = 0
$failed = 0
$skipped = 0

for ($i = 0; $i -lt $total; $i++) {
    $num = $i + 1
    $r = $Repos[$i]
    $repoFullName = "$OrgName/$($r.Repo)"
    $srcPath = Join-Path $SourceRoot $r.Source
    $description = Get-RepoDescription $r.Source

    Write-Host "`n[$num/$total] $repoFullName" -ForegroundColor Cyan
    Write-Host "  Source:      $($r.Source)"
    Write-Host "  Platform:    $($r.Platform)"
    Write-Host "  Description: $description"

    # Skip if before StartAt
    if ($num -lt $StartAt) {
        Write-Host "  SKIPPED (before StartAt=$StartAt)" -ForegroundColor DarkGray
        $skipped++
        continue
    }

    # Skip if source missing
    if (-not (Test-Path $srcPath)) {
        Write-Host "  SKIPPED (source directory missing)" -ForegroundColor Yellow
        $skipped++
        continue
    }

    if ($DryRun) {
        Write-Host "  [DRY RUN] Would create repo: $repoFullName ($Visibility)" -ForegroundColor Yellow
        Write-Host "  [DRY RUN] Would copy from: $srcPath" -ForegroundColor Yellow
        Write-Host "  [DRY RUN] Would ensure .gitignore for $($r.Platform)" -ForegroundColor Yellow
        Write-Host "  [DRY RUN] Would git init, commit, push to origin main" -ForegroundColor Yellow
        $succeeded++
        continue
    }

    # ── Create GitHub repo ───────────────────────────────────────────────
    try {
        Write-Host "  Creating repo..." -NoNewline
        $createOutput = gh repo create $repoFullName `
            --$Visibility `
            --description $description `
            --confirm 2>&1

        if ($LASTEXITCODE -ne 0) {
            # Check if it already exists
            if ($createOutput -match "already exists") {
                Write-Host " already exists, continuing." -ForegroundColor Yellow
            } else {
                throw "gh repo create failed: $createOutput"
            }
        } else {
            Write-Host " done." -ForegroundColor Green
        }
    } catch {
        Write-Host " FAILED" -ForegroundColor Red
        Write-Warning "  Error creating repo: $_"
        $failed++
        continue
    }

    # ── Prepare temp directory ───────────────────────────────────────────
    $workDir = Join-Path $TempRoot $r.Repo
    if (Test-Path $workDir) {
        Remove-Item -Recurse -Force $workDir
    }
    New-Item -ItemType Directory -Path $workDir -Force | Out-Null

    # ── Copy files (excluding build artifacts) ───────────────────────────
    try {
        Write-Host "  Copying files..." -NoNewline

        # Use robocopy for efficient copy with exclusions
        $robocopyArgs = @(
            $srcPath,
            $workDir,
            "/E",           # Recurse
            "/NFL",         # No file list
            "/NDL",         # No directory list
            "/NJH",         # No job header
            "/NJS",         # No job summary
            "/NC",          # No file class
            "/NS"           # No file size
        )
        foreach ($d in $ExcludeDirs) {
            $robocopyArgs += "/XD"
            $robocopyArgs += $d
        }
        foreach ($f in $ExcludeFiles) {
            $robocopyArgs += "/XF"
            $robocopyArgs += $f
        }

        $null = & robocopy @robocopyArgs
        # Robocopy exit codes 0-7 are success
        if ($LASTEXITCODE -gt 7) {
            throw "robocopy failed with exit code $LASTEXITCODE"
        }

        Write-Host " done." -ForegroundColor Green
    } catch {
        Write-Host " FAILED" -ForegroundColor Red
        Write-Warning "  Error copying files: $_"
        $failed++
        continue
    }

    # ── Ensure .gitignore exists ─────────────────────────────────────────
    $gitignorePath = Join-Path $workDir ".gitignore"
    if (-not (Test-Path $gitignorePath)) {
        Write-Host "  Adding .gitignore for $($r.Platform)..." -NoNewline
        switch ($r.Platform) {
            "aspnet" {
                $rootGitignore = Join-Path $SourceRoot ".gitignore"
                if (Test-Path $rootGitignore) {
                    Copy-Item $rootGitignore $gitignorePath
                }
            }
            "node" {
                Set-Content -Path $gitignorePath -Value $NodeGitignore -NoNewline
            }
            "jsp" {
                Set-Content -Path $gitignorePath -Value $JspGitignore -NoNewline
            }
        }
        Write-Host " done." -ForegroundColor Green
    } else {
        Write-Host "  .gitignore already present." -ForegroundColor DarkGray
    }

    # ── Git init, commit, push ───────────────────────────────────────────
    try {
        Write-Host "  Initializing git..." -NoNewline
        Push-Location $workDir

        git init --initial-branch=main 2>&1 | Out-Null
        git add -A 2>&1 | Out-Null
        git commit -m "Initial commit from fiasse_benchmark_output" 2>&1 | Out-Null

        $remoteUrl = "https://github.com/$repoFullName.git"
        git remote add origin $remoteUrl 2>&1 | Out-Null
        git push -u origin main 2>&1 | Out-Null

        if ($LASTEXITCODE -ne 0) {
            throw "git push failed"
        }

        Pop-Location
        Write-Host " done." -ForegroundColor Green
        $succeeded++
    } catch {
        Pop-Location
        Write-Host " FAILED" -ForegroundColor Red
        Write-Warning "  Error in git operations: $_"
        $failed++
        continue
    }

    # ── Cleanup temp ─────────────────────────────────────────────────────
    if (Test-Path $workDir) {
        Remove-Item -Recurse -Force $workDir
    }
}

# ── Summary ──────────────────────────────────────────────────────────────────
Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "  Total:     $total"
Write-Host "  Succeeded: $succeeded" -ForegroundColor Green
Write-Host "  Failed:    $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
Write-Host "  Skipped:   $skipped" -ForegroundColor $(if ($skipped -gt 0) { "Yellow" } else { "Green" })

if (-not $DryRun -and $succeeded -gt 0) {
    Write-Host "`nVerify with:" -ForegroundColor Cyan
    Write-Host "  gh repo list $OrgName --limit 30"
}

if ($failed -gt 0) {
    Write-Host "`nTo retry failed repos, re-run with -StartAt targeting the first failure." -ForegroundColor Yellow
    exit 1
}
