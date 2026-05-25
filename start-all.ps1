# ============================================================
#  E-Commerce Microsservicos - Script de Inicializacao
# ============================================================
#
#  USO:
#    .\start-all.ps1                -> Sobe tudo
#    .\start-all.ps1 -StopAll       -> Para tudo
#    .\start-all.ps1 -SkipDocker    -> Pula docker-compose up
#    .\start-all.ps1 -SkipFrontend  -> Pula o frontend React
#    .\start-all.ps1 -SkipBuild     -> Pula mvn install do grpc-contracts
#
#  ORDEM:
#    1. Docker    -> PostgreSQL (5432) + RabbitMQ (5672/15672) + PgAdmin (5050)
#    2. gRPC      -> mvn install em grpc-contracts
#    3. Eureka    -> eureka-server (8761)
#    4. Camada 1  -> produto-service (8087) + pedido-service (8082/gRPC:9090) [paralelo]
#    5. Camada 2  -> carrinho (8083) + pagamento (8084) + estoque (8085) + notificacao (8086) [paralelo]
#    6. Frontend  -> React (3000)
# ============================================================

param(
    [switch]$StopAll,
    [switch]$SkipDocker,
    [switch]$SkipFrontend,
    [switch]$SkipBuild
)

# ── Helpers de output colorido ────────────────────────────────
function Write-OK   { param([string]$m) Write-Host "  [OK]  $m" -ForegroundColor Green }
function Write-ERR  { param([string]$m) Write-Host "  [ERRO] $m" -ForegroundColor Red }
function Write-WARN { param([string]$m) Write-Host "  [AVISO] $m" -ForegroundColor Yellow }
function Write-INFO { param([string]$m) Write-Host "  [INFO] $m" -ForegroundColor Cyan }
function Write-WAIT { param([string]$m) Write-Host "  [AGUARDANDO] $m" -ForegroundColor Magenta }
function Write-STEP { param([int]$n, [string]$m)
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Blue
    Write-Host "  [$n/6] $m" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Blue
}

# ── Constantes ────────────────────────────────────────────────
$ROOT   = $PSScriptRoot
$LOGS   = Join-Path $ROOT "logs"
$PIDS_F = Join-Path $ROOT ".running-pids"

if (-not (Test-Path $LOGS)) { New-Item -ItemType Directory -Path $LOGS | Out-Null }

# ── Banner ────────────────────────────────────────────────────
function Show-Banner {
    Write-Host ""
    Write-Host "  ===========================================================" -ForegroundColor Cyan
    Write-Host "     E-Commerce Microsservicos  -  Startup Script"             -ForegroundColor Cyan
    Write-Host "  ===========================================================" -ForegroundColor Cyan
    Write-Host ""
}

# ════════════════════════════════════════════════════════════════
#  MODO STOP
# ════════════════════════════════════════════════════════════════
if ($StopAll) {
    Show-Banner
    Write-Host "  Parando todos os servicos..." -ForegroundColor Red
    Write-Host ""

    if (Test-Path $PIDS_F) {
        $savedPids = Get-Content $PIDS_F
        foreach ($pid in $savedPids) {
            if ($pid -match '^\d+$') {
                $p = Get-Process -Id ([int]$pid) -ErrorAction SilentlyContinue
                if ($p) {
                    Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
                    Write-OK "PID $pid ($($p.ProcessName)) finalizado"
                }
            }
        }
        Remove-Item $PIDS_F -Force
    } else {
        Write-WARN "Nenhum arquivo .running-pids encontrado"
    }

    Write-WAIT "Parando containers Docker..."
    Set-Location $ROOT
    docker-compose down 2>&1 | Out-Null
    Write-OK "Docker containers parados"
    Write-Host ""
    Write-Host "  Tudo parado! Tchau." -ForegroundColor Green
    Write-Host ""
    exit 0
}

# ════════════════════════════════════════════════════════════════
#  Funcoes auxiliares
# ════════════════════════════════════════════════════════════════
function Wait-Port {
    param([int]$Port, [string]$Name, [int]$Timeout = 150)
    $elapsed = 0
    $step = 3
    while ($elapsed -lt $Timeout) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $r = $tcp.BeginConnect("localhost", $Port, $null, $null)
            $ok = $r.AsyncWaitHandle.WaitOne(1500, $false)
            $tcp.Close()
            if ($ok) {
                Write-OK "$Name UP na porta $Port (${elapsed}s)"
                return $true
            }
        } catch { }
        Start-Sleep $step
        $elapsed += $step
    }
    Write-ERR "$Name NAO subiu em ${Timeout}s na porta $Port!"
    return $false
}

function Start-Svc {
    param([string]$Dir, [string]$Name)
    $logOut = Join-Path $LOGS "$Name.log"
    $logErr = Join-Path $LOGS "$Name.err.log"
    $fullDir = Join-Path $ROOT $Dir
    $p = Start-Process "mvn" `
        -ArgumentList "spring-boot:run", "-q" `
        -WorkingDirectory $fullDir `
        -RedirectStandardOutput $logOut `
        -RedirectStandardError  $logErr `
        -PassThru -WindowStyle Hidden
    Add-Content $PIDS_F -Value $p.Id
    Write-INFO "$Name iniciado (PID $($p.Id)) | log -> logs\$Name.log"
    return $p
}

function Check-Ports-Parallel {
    param([hashtable[]]$Services, [int]$Timeout = 180)
    $jobs = @()
    foreach ($s in $Services) {
        $port = $s.port
        $name = $s.name
        $jobs += Start-Job -ScriptBlock {
            param($p, $n, $t)
            $e = 0
            $step = 3
            while ($e -lt $t) {
                try {
                    $tcp = New-Object System.Net.Sockets.TcpClient
                    $r = $tcp.BeginConnect("localhost", $p, $null, $null)
                    $ok = $r.AsyncWaitHandle.WaitOne(1500, $false)
                    $tcp.Close()
                    if ($ok) {
                        return @{ ok = $true; name = $n; port = $p; elapsed = $e }
                    }
                } catch { }
                Start-Sleep $step
                $e += $step
            }
            return @{ ok = $false; name = $n; port = $p; elapsed = $e }
        } -ArgumentList $port, $name, $Timeout
    }

    Write-WAIT "Aguardando servicos subirem em paralelo (pode levar alguns minutos)..."
    $results = $jobs | ForEach-Object { $_ | Wait-Job -Timeout ($Timeout + 15) | Receive-Job }
    $jobs | Remove-Job -Force

    foreach ($res in $results) {
        if ($res.ok) {
            Write-OK "$($res.name) UP na porta $($res.port) ($($res.elapsed)s)"
        } else {
            Write-ERR "$($res.name) NAO subiu! Verifique: logs\$($res.name).log"
        }
    }
}

# ════════════════════════════════════════════════════════════════
#  INICIO
# ════════════════════════════════════════════════════════════════
if (Test-Path $PIDS_F) { Remove-Item $PIDS_F -Force }

Show-Banner
Write-Host "  Projeto : $ROOT" -ForegroundColor Gray
Write-Host "  Logs    : $LOGS" -ForegroundColor Gray
Write-Host "  Inicio  : $(Get-Date -Format 'dd/MM/yyyy HH:mm:ss')" -ForegroundColor Gray

# ──────────────────────────────────────────────────────────────
# [1/6] Docker
# ──────────────────────────────────────────────────────────────
if (-not $SkipDocker) {
    Write-STEP 1 "Infraestrutura Docker (PostgreSQL + RabbitMQ + PgAdmin)"
    Set-Location $ROOT
    Write-INFO "Executando docker-compose up -d..."
    $out = docker-compose up -d 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-ERR "docker-compose falhou!"
        $out | ForEach-Object { Write-Host "    $_" }
        exit 1
    }
    Write-OK "Containers iniciados"
    $null = Wait-Port -Port 5432 -Name "PostgreSQL" -Timeout 90
    $null = Wait-Port -Port 5672 -Name "RabbitMQ"   -Timeout 90
    Write-Host ""
    Write-INFO "PgAdmin     -> http://localhost:5050  (admin@admin.com / admin)"
    Write-INFO "RabbitMQ UI -> http://localhost:15672 (guest / guest)"
} else {
    Write-STEP 1 "Infraestrutura Docker"
    Write-WARN "Flag -SkipDocker ativa - pulando docker-compose"
}

# ──────────────────────────────────────────────────────────────
# [2/6] Build gRPC Contracts
# ──────────────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-STEP 2 "Build dos Contratos gRPC (grpc-contracts)"
    $grpcDir = Join-Path $ROOT "grpc-contracts"
    $grpcLog = Join-Path $LOGS "grpc-contracts.log"
    Write-INFO "Executando mvn install em grpc-contracts..."
    $p = Start-Process "mvn" `
        -ArgumentList "install", "-q" `
        -WorkingDirectory $grpcDir `
        -RedirectStandardOutput $grpcLog `
        -RedirectStandardError  "$grpcLog.err" `
        -PassThru -Wait -WindowStyle Hidden
    if ($p.ExitCode -eq 0) {
        Write-OK "grpc-contracts instalado no repositorio Maven local"
    } else {
        Write-WARN "mvn install falhou (exit $($p.ExitCode)) - pode ja estar instalado"
        Write-WARN "Verifique: logs\grpc-contracts.log"
    }
} else {
    Write-STEP 2 "Build gRPC Contracts"
    Write-WARN "Flag -SkipBuild ativa - pulando build dos contratos"
}

# ──────────────────────────────────────────────────────────────
# [3/6] Eureka Server
# ──────────────────────────────────────────────────────────────
Write-STEP 3 "Eureka Server - Service Discovery (porta 8761)"
$null = Start-Svc "eureka-server" "eureka-server"
$eurekaOk = Wait-Port -Port 8761 -Name "Eureka Server" -Timeout 150
if (-not $eurekaOk) {
    Write-ERR "Eureka nao subiu! Os outros servicos vao falhar ao registrar."
    Write-WARN "Verifique: logs\eureka-server.log"
} else {
    Write-INFO "Eureka Dashboard -> http://localhost:8761"
}

# ──────────────────────────────────────────────────────────────
# [4/6] Camada 1 - produto + pedido
# ──────────────────────────────────────────────────────────────
Write-STEP 4 "produto-service (8087) + pedido-service (8082 / gRPC:9090)"
$null = Start-Svc "produto-service" "produto-service"
$null = Start-Svc "pedido-service"  "pedido-service"
Check-Ports-Parallel @(
    @{ name = "produto-service"; port = 8087 },
    @{ name = "pedido-service";  port = 8082 }
) -Timeout 180

# ──────────────────────────────────────────────────────────────
# [5/6] Camada 2 - carrinho + pagamento + estoque + notificacao
# ──────────────────────────────────────────────────────────────
Write-STEP 5 "carrinho(8083) + pagamento(8084) + estoque(8085) + notificacao(8086)"
$null = Start-Svc "carrinho-service"    "carrinho-service"
$null = Start-Svc "pagamento-service"   "pagamento-service"
$null = Start-Svc "estoque-service"     "estoque-service"
$null = Start-Svc "notificacao-service" "notificacao-service"
Check-Ports-Parallel @(
    @{ name = "carrinho-service";    port = 8083 },
    @{ name = "pagamento-service";   port = 8084 },
    @{ name = "estoque-service";     port = 8085 },
    @{ name = "notificacao-service"; port = 8086 }
) -Timeout 180

# ──────────────────────────────────────────────────────────────
# [6/6] Frontend React
# ──────────────────────────────────────────────────────────────
if (-not $SkipFrontend) {
    Write-STEP 6 "Frontend React (porta 3000)"
    $frontDir = Join-Path $ROOT "frontend"
    $frontLog = Join-Path $LOGS "frontend.log"
    $nmDir    = Join-Path $frontDir "node_modules"

    if (-not (Test-Path $nmDir)) {
        Write-INFO "node_modules nao encontrado - executando npm install..."
        $ni = Start-Process "cmd" `
            -ArgumentList "/c", "npm install" `
            -WorkingDirectory $frontDir `
            -RedirectStandardOutput "$frontLog.install" `
            -RedirectStandardError  "$frontLog.install.err" `
            -PassThru -Wait -WindowStyle Hidden
        if ($ni.ExitCode -eq 0) {
            Write-OK "npm install concluido"
        } else {
            Write-ERR "npm install falhou! Verifique: logs\frontend.log.install"
        }
    } else {
        Write-INFO "node_modules ja existe - pulando npm install"
    }

    Write-INFO "Iniciando npm start..."
    $fp = Start-Process "cmd" `
        -ArgumentList "/c", "npm start" `
        -WorkingDirectory $frontDir `
        -RedirectStandardOutput $frontLog `
        -RedirectStandardError  "$frontLog.err" `
        -PassThru -WindowStyle Hidden
    Add-Content $PIDS_F -Value $fp.Id
    $null = Wait-Port -Port 3000 -Name "Frontend React" -Timeout 120
} else {
    Write-STEP 6 "Frontend React"
    Write-WARN "Flag -SkipFrontend ativa - pulando frontend"
}

# ════════════════════════════════════════════════════════════════
#  RESUMO FINAL
# ════════════════════════════════════════════════════════════════
Write-Host ""
Write-Host "  ===========================================================" -ForegroundColor Green
Write-Host "     STACK COMPLETO INICIALIZADO!"                             -ForegroundColor Green
Write-Host "  ===========================================================" -ForegroundColor Green
Write-Host "  PostgreSQL       -> localhost:5432"                          -ForegroundColor Cyan
Write-Host "  RabbitMQ         -> http://localhost:15672 (guest/guest)"   -ForegroundColor Cyan
Write-Host "  PgAdmin          -> http://localhost:5050  (admin@admin.com/admin)" -ForegroundColor Cyan
Write-Host "  Eureka Dashboard -> http://localhost:8761"                  -ForegroundColor Cyan
Write-Host "  produto-service  -> http://localhost:8087"                  -ForegroundColor Cyan
Write-Host "  pedido-service   -> http://localhost:8082"                  -ForegroundColor Cyan
Write-Host "  carrinho-service -> http://localhost:8083"                  -ForegroundColor Cyan
Write-Host "  pagamento-svc    -> http://localhost:8084"                  -ForegroundColor Cyan
Write-Host "  estoque-service  -> http://localhost:8085"                  -ForegroundColor Cyan
Write-Host "  notificacao-svc  -> http://localhost:8086"                  -ForegroundColor Cyan
Write-Host "  Frontend React   -> http://localhost:3000"                  -ForegroundColor Cyan
Write-Host "  ===========================================================" -ForegroundColor Green
Write-Host "  Logs em: $LOGS"                                             -ForegroundColor Yellow
Write-Host "  Para parar tudo: .\start-all.ps1 -StopAll"                 -ForegroundColor Yellow
Write-Host "  ===========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  [SCRIPT BLOQUEADO] Mantendo os processos vivos no background..." -ForegroundColor Magenta

# Mantenha o script rodando para evitar que o Job do Runner mate os processos filhos
while ($true) {
    Start-Sleep -Seconds 10
}
