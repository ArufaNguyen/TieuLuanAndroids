@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"

title Smart Calendar Backend Environment Installer

net session >nul 2>&1
if errorlevel 1 (
    echo Requesting Administrator permission...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -WorkingDirectory '%~dp0' -Verb RunAs"
    exit /b
)

echo ============================================================
echo   Smart Calendar Backend Environment Installer
echo ============================================================
echo.

if not exist "backend\SmartCalendarAPI\build.gradle.kts" (
    echo [ERROR] Missing backend\SmartCalendarAPI.
    echo This installer must be run from the full project containing the backend.
    goto :failed
)

echo Choose database mode:
echo   1. docker  - Use Docker Desktop + MSSQL container
echo   2. direct  - Use SQL Server installed directly on this VPS/machine
echo.
set "HYPERVISOR="
set /p "HYPERVISOR=Enter mode [docker/direct, default docker]: "
if not defined HYPERVISOR set "HYPERVISOR=docker"
if /i "%HYPERVISOR%"=="1" set "HYPERVISOR=docker"
if /i "%HYPERVISOR%"=="2" set "HYPERVISOR=direct"
if /i not "%HYPERVISOR%"=="docker" if /i not "%HYPERVISOR%"=="direct" (
    echo [ERROR] Invalid mode: %HYPERVISOR%
    goto :failed
)

for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')"`) do set "PATH=%%P"

where winget >nul 2>&1
if errorlevel 1 (
    echo [WARN] winget is not installed. Automatic software install will be skipped.
    echo Install JDK 21 manually, and Docker Desktop only if using docker mode.
) else (
    call :ensure_java_available
    if errorlevel 1 (
        call :install_package "EclipseAdoptium.Temurin.21.JDK" "JDK 21"
        if errorlevel 1 goto :failed
        for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')"`) do set "PATH=%%P"
        call :ensure_java_available
        if errorlevel 1 goto :failed
    )

    where cloudflared >nul 2>&1
    if errorlevel 1 (
        call :install_package "Cloudflare.cloudflared" "Cloudflared"
        if errorlevel 1 goto :failed
        for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')"`) do set "PATH=%%P"
    ) else (
        echo [OK] Cloudflared is already available.
    )
    set "DEFAULT_AUTO_START_TUNNEL=true"

    if /i "%HYPERVISOR%"=="docker" (
        where docker >nul 2>&1
        if errorlevel 1 (
            if exist "C:\Program Files\Docker\Docker\resources\bin\docker.exe" set "PATH=%PATH%;C:\Program Files\Docker\Docker\resources\bin"
        )
        where docker >nul 2>&1
        if errorlevel 1 (
            call :install_package "Docker.DockerDesktop" "Docker Desktop"
            if errorlevel 1 goto :failed
        ) else (
            echo [OK] Docker CLI is already available.
        )
    )
)

for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')"`) do set "PATH=%%P"
call :ensure_java_available
if errorlevel 1 goto :failed

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not available in PATH. Install JDK 21, then run this installer again.
    goto :failed
)

echo.
echo [SETUP] Creating local environment files...

if /i "%HYPERVISOR%"=="direct" (
    set "DEFAULT_DB_PORT=1433"
) else (
    set "DEFAULT_DB_PORT=6969"
)

if not exist "backend\SmartCalendarAPI\.env" (
    set "DB_PASSWORD="
    set /p "DB_PASSWORD=Enter a strong MSSQL sa password: "
    if not defined DB_PASSWORD (
        echo [ERROR] MSSQL sa password cannot be empty.
        goto :failed
    )

    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$content = @(('HYPERVISOR=' + $env:HYPERVISOR),'DB_HOST=localhost',('DB_PORT=' + $env:DEFAULT_DB_PORT),'DB_NAME=SmartCalendarDB','DB_USERNAME=sa',('DB_PASSWORD=' + $env:DB_PASSWORD),'','SERVER_PORT=7923','','DEV_MODE=true',('AUTO_START_TUNNEL=' + $env:DEFAULT_AUTO_START_TUNNEL),'HAR_MAX_FILE_SIZE=30MB','','NINEROUTER_URL=http://localhost:20128','NINEROUTER_KEY=','','FOZA_BASE_URL=https://api.foza.ai/v1','FOZA_API_KEY=','','# Agent model routing','NINEROUTER_PALAMEDES_MODEL=openrouter-combo','NINEROUTER_PERCIVAL_MODEL=openrouter-combo','NINEROUTER_KAY_MODEL=openrouter-combo','NINEROUTER_MORGAN_MODEL=opencode-combo','NINEROUTER_AGENT_CHAT_MODEL=openrouter-combo','NINEROUTER_AGENT_CHAT_ENABLED=true','FOZA_MERLIN_MODEL=hoang/claude-sonnet-4.6','FOZA_MERLIN_FALLBACK_MODEL=hoang/gpt-5.5','# Local/no-model agents: GalahadCollectorAgent, GawainSafetyAgent, ScheduleSignalAgent, ArthurJudgeAgent, MordredReplayGatekeeper, BedivereToolDesignerAgent'); [IO.File]::WriteAllLines('backend\SmartCalendarAPI\.env', $content)"

    if errorlevel 1 (
        echo [ERROR] Could not create backend .env.
        goto :failed
    )
    echo [OK] Created backend\SmartCalendarAPI\.env
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$path='backend\SmartCalendarAPI\.env'; $lines=[System.Collections.Generic.List[string]](Get-Content $path); function Upsert($key,$value){ $idx=-1; for($i=0;$i -lt $lines.Count;$i++){ if($lines[$i] -match ('^' + [regex]::Escape($key) + '=')){ $idx=$i; break } }; if($idx -ge 0){ $lines[$idx]=$key + '=' + $value } else { $lines.Add($key + '=' + $value) } }; function Ensure($key,$value){ $idx=-1; for($i=0;$i -lt $lines.Count;$i++){ if($lines[$i] -match ('^' + [regex]::Escape($key) + '=')){ $idx=$i; break } }; if($idx -lt 0 -or [string]::IsNullOrWhiteSpace($lines[$idx].Substring($lines[$idx].IndexOf('=') + 1))){ Upsert $key $value } }; function Remove($key){ for($i=$lines.Count-1;$i -ge 0;$i--){ if($lines[$i] -match ('^' + [regex]::Escape($key) + '=')){ $lines.RemoveAt($i) } } }; function EnsureComment($value){ if(-not ($lines -contains $value)){ $lines.Add($value) } }; Upsert 'HYPERVISOR' $env:HYPERVISOR; Upsert 'DB_HOST' 'localhost'; Upsert 'DB_PORT' $env:DEFAULT_DB_PORT; Ensure 'DB_NAME' 'SmartCalendarDB'; Ensure 'DB_USERNAME' 'sa'; Ensure 'SERVER_PORT' '7923'; Ensure 'DEV_MODE' 'true'; Ensure 'AUTO_START_TUNNEL' $env:DEFAULT_AUTO_START_TUNNEL; Ensure 'HAR_MAX_FILE_SIZE' '30MB'; Ensure 'NINEROUTER_URL' 'http://localhost:20128'; Ensure 'NINEROUTER_KEY' ''; Ensure 'FOZA_BASE_URL' 'https://api.foza.ai/v1'; Ensure 'FOZA_API_KEY' ''; for($i=$lines.Count-1;$i -ge 0;$i--){ if($lines[$i] -match '^((?:NINEROUTER|FOZA)_[A-Z0-9_]*(?:MODEL|COMBO))=' -and $matches[1] -notin @('NINEROUTER_PALAMEDES_MODEL','NINEROUTER_PERCIVAL_MODEL','NINEROUTER_KAY_MODEL','NINEROUTER_MORGAN_MODEL','NINEROUTER_AGENT_CHAT_MODEL','FOZA_MERLIN_MODEL','FOZA_MERLIN_FALLBACK_MODEL')){ $lines.RemoveAt($i) } }; EnsureComment ''; EnsureComment '# Agent model routing'; Ensure 'NINEROUTER_PALAMEDES_MODEL' 'openrouter-combo'; Ensure 'NINEROUTER_PERCIVAL_MODEL' 'openrouter-combo'; Ensure 'NINEROUTER_KAY_MODEL' 'openrouter-combo'; Ensure 'NINEROUTER_MORGAN_MODEL' 'opencode-combo'; Ensure 'NINEROUTER_AGENT_CHAT_MODEL' 'openrouter-combo'; Ensure 'NINEROUTER_AGENT_CHAT_ENABLED' 'true'; Ensure 'FOZA_MERLIN_MODEL' 'hoang/claude-sonnet-4.6'; Ensure 'FOZA_MERLIN_FALLBACK_MODEL' 'hoang/gpt-5.5'; EnsureComment '# Local/no-model agents: GalahadCollectorAgent, GawainSafetyAgent, ScheduleSignalAgent, ArthurJudgeAgent, MordredReplayGatekeeper, BedivereToolDesignerAgent'; [IO.File]::WriteAllLines($path,$lines)"
    echo [OK] Updated database mode and ensured required backend settings in backend\SmartCalendarAPI\.env
)

if exist "backend\ReverseAPIEndpoint" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$source='backend\SmartCalendarAPI\.env'; $target='backend\ReverseAPIEndpoint\.env'; $wanted='FOZA_BASE_URL','FOZA_API_KEY','FOZA_MERLIN_MODEL','FOZA_MERLIN_FALLBACK_MODEL','NINEROUTER_URL','NINEROUTER_KEY','NINEROUTER_PALAMEDES_MODEL','NINEROUTER_PERCIVAL_MODEL','NINEROUTER_KAY_MODEL','NINEROUTER_MORGAN_MODEL','NINEROUTER_AGENT_CHAT_MODEL','NINEROUTER_AGENT_CHAT_ENABLED'; $map=@{}; Get-Content $source | ForEach-Object { if($_ -match '^([^#=]+)=(.*)$'){ $map[$matches[1]]=$matches[2] } }; $lines=@(); foreach($key in $wanted){ if($map.ContainsKey($key)){ $lines += ($key + '=' + $map[$key]) } }; if($lines.Count -gt 0){ [IO.File]::WriteAllLines($target,$lines) }"
    echo [OK] Synced ReverseAPIEndpoint runtime .env values from backend .env
)

if exist "tunnel-url-publisher" (
    if not exist "tunnel-url-publisher\.env" (
        if exist "tunnel-url-publisher\.env.example" (
            copy /y "tunnel-url-publisher\.env.example" "tunnel-url-publisher\.env" >nul
        ) else (
            > "tunnel-url-publisher\.env" echo GITHUB_TOKEN=your_github_token_here
            >> "tunnel-url-publisher\.env" echo TUNNEL_BACKEND_URL=http://localhost:7923
        )
        echo [ACTION REQUIRED] Set GITHUB_TOKEN in tunnel-url-publisher\.env
    ) else (
        echo [OK] tunnel-url-publisher\.env already exists.
    )
)

set "DB_PASSWORD="
set "DB_PORT="
set "DB_NAME="
set "DB_USERNAME="
set "SERVER_PORT="
for /f "usebackq tokens=1,* delims==" %%A in ("backend\SmartCalendarAPI\.env") do (
    if /i "%%A"=="DB_PASSWORD" set "DB_PASSWORD=%%B"
    if /i "%%A"=="DB_PORT" set "DB_PORT=%%B"
    if /i "%%A"=="DB_NAME" set "DB_NAME=%%B"
    if /i "%%A"=="DB_USERNAME" set "DB_USERNAME=%%B"
    if /i "%%A"=="SERVER_PORT" set "SERVER_PORT=%%B"
)
if not defined DB_PASSWORD (
    echo [ERROR] DB_PASSWORD is missing from backend\SmartCalendarAPI\.env
    goto :failed
)
if not defined DB_PORT set "DB_PORT=%DEFAULT_DB_PORT%"
if not defined DB_NAME set "DB_NAME=SmartCalendarDB"
if not defined DB_USERNAME set "DB_USERNAME=sa"
if not defined SERVER_PORT set "SERVER_PORT=7923"

if /i "%HYPERVISOR%"=="direct" goto :direct_mode

echo.
echo [SETUP] Starting Docker Desktop...

docker info >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\Docker\Docker\Docker Desktop.exe" (
        start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    ) else (
        echo [ERROR] Docker Desktop was not found.
        echo If this VPS does not support virtualization, rerun installer and choose direct mode.
        goto :failed
    )

    echo Waiting for Docker daemon...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ready = $false; 1..90 | ForEach-Object { docker info *> $null; if ($LASTEXITCODE -eq 0) { $ready = $true; break }; Start-Sleep -Seconds 2 }; if (-not $ready) { exit 1 }"

    if errorlevel 1 (
        echo [ERROR] Docker did not become ready within 180 seconds.
        echo If virtualization is not available, rerun installer and choose direct mode.
        goto :failed
    )
)
echo [OK] Docker daemon is running.

docker inspect TLANDROIDserver >nul 2>&1
if errorlevel 1 (
    echo [SETUP] Creating MSSQL container TLANDROIDserver...
    docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=%DB_PASSWORD%" -p %DB_PORT%:1433 --name TLANDROIDserver -v mssql_data:/var/opt/mssql -d mcr.microsoft.com/mssql/server:2022-latest
    if errorlevel 1 (
        echo [ERROR] Could not create MSSQL container.
        goto :failed
    )
) else (
    echo [SETUP] Starting existing MSSQL container TLANDROIDserver...
    docker start TLANDROIDserver >nul
    if errorlevel 1 (
        echo [ERROR] Could not start MSSQL container.
        goto :failed
    )
)

echo [SETUP] Waiting for MSSQL...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ready = $false; 1..60 | ForEach-Object { docker exec TLANDROIDserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U $env:DB_USERNAME -P $env:DB_PASSWORD -Q 'SELECT 1' *> $null; if ($LASTEXITCODE -eq 0) { $ready = $true; break }; Start-Sleep -Seconds 2 }; if (-not $ready) { exit 1 }"

if errorlevel 1 (
    echo [ERROR] MSSQL did not become ready. Check that DB_PASSWORD matches the container password.
    goto :failed
)

docker exec TLANDROIDserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U "%DB_USERNAME%" -P "%DB_PASSWORD%" -Q "IF DB_ID(N'%DB_NAME%') IS NULL CREATE DATABASE [%DB_NAME%]"
if errorlevel 1 (
    echo [ERROR] Could not create %DB_NAME%.
    goto :failed
)

echo.
echo [SETUP] Creating/updating SmartCalendarDB tables and seed data...
call setup-smart-calendar-database.bat
if errorlevel 1 (
    echo [ERROR] Database setup failed.
    goto :failed
)
echo [OK] SmartCalendarDB tables and seed data are ready.
goto :verify

:direct_mode
echo.
echo [DIRECT MODE] Docker will not be used.
call :ensure_direct_sql_server
if errorlevel 1 goto :failed

echo.
echo [SETUP] Creating/updating SmartCalendarDB...
call setup-smart-calendar-database.bat
if errorlevel 1 (
    echo [ERROR] Database setup failed.
    goto :failed
)
echo [OK] SmartCalendarDB is ready.
goto :verify

:verify
echo.
echo [VERIFY] Java version:
java -version

echo.
echo [VERIFY] Building backend...
call gradlew.bat -PskipAndroidApp=true :backend:build --console=plain
if errorlevel 1 (
    echo [ERROR] Backend build failed.
    goto :failed
)

echo.
echo [SETUP] Opening Windows Firewall for backend port %SERVER_PORT%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$port=[int]$env:SERVER_PORT; $name='SmartCalendar Backend ' + $port; if(-not (Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue)){ New-NetFirewallRule -DisplayName $name -Direction Inbound -Action Allow -Protocol TCP -LocalPort $port | Out-Null }; exit 0"
if errorlevel 1 (
    echo [WARN] Could not create firewall rule automatically. Open TCP port %SERVER_PORT% manually if remote clients cannot connect.
) else (
    echo [OK] Firewall rule is ready for TCP port %SERVER_PORT%.
)

echo.
echo ============================================================
echo   Installation completed successfully
echo ============================================================
echo.
echo HYPERVISOR=%HYPERVISOR%
echo.
if /i "%HYPERVISOR%"=="docker" (
    echo Run backend:
    echo   gradlew.bat -PskipAndroidApp=true :backend:bootRun --console=plain
) else (
    echo Run backend without Docker:
    echo   gradlew.bat -PskipAndroidApp=true :backend:bootJar --console=plain
    echo   java -jar backend\SmartCalendarAPI\build\libs\backend-0.0.1-SNAPSHOT.jar
)
echo.
echo Backend URL:
echo   http://localhost:%SERVER_PORT%
echo Analyze HAR route:
echo   http://localhost:%SERVER_PORT%/api/v1/analyze/
echo.
pause
exit /b 0

:install_package
echo.
echo [INSTALL] %~2...
set "PKG_ID=%~1"
set "PKG_TIMEOUT=1200"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$id=$env:PKG_ID; $timeout=[int]$env:PKG_TIMEOUT; $args=@('install','--id',$id,'--exact','--silent','--accept-package-agreements','--accept-source-agreements','--disable-interactivity'); $p=Start-Process -FilePath 'winget' -ArgumentList $args -PassThru; if(-not $p.WaitForExit($timeout * 1000)){ try { Stop-Process -Id $p.Id -Force } catch {}; Write-Host ('[ERROR] winget timeout after ' + $timeout + ' seconds.'); exit 124 }; exit $p.ExitCode"
if errorlevel 1 (
    echo [ERROR] Failed to install %~2.
    exit /b 1
)
echo [OK] %~2 is installed.
exit /b 0

:ensure_java_available
where java >nul 2>&1
if not errorlevel 1 (
    echo [OK] Java is already available.
    exit /b 0
)

for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$roots=@('C:\Program Files\Eclipse Adoptium','C:\Program Files\Java','D:\Java'); foreach($root in $roots){ if(Test-Path $root){ $java=Get-ChildItem -LiteralPath $root -Recurse -Filter java.exe -ErrorAction SilentlyContinue | Where-Object { $_.FullName -match '\\bin\\java\.exe$' } | Sort-Object FullName -Descending | Select-Object -First 1; if($java){ Split-Path (Split-Path $java.FullName -Parent) -Parent; exit 0 } } }; exit 1"`) do set "JAVA_HOME=%%J"

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo [OK] Java found at %JAVA_HOME%
    exit /b 0
)

echo [WARN] Java is not available yet.
exit /b 1

:ensure_direct_sql_server
echo.
echo [SETUP] Checking direct SQL Server installation...

powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-Service MSSQLSERVER -ErrorAction SilentlyContinue) { exit 0 } elseif (Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if not errorlevel 1 (
    echo [OK] SQL Server service already exists.
    goto :configure_direct_sql_server
)

where winget >nul 2>&1
if errorlevel 1 (
    echo [ERROR] winget is required to auto-install SQL Server Express in direct mode.
    echo Install winget or install SQL Server Express manually, then run this file again.
    exit /b 1
)

echo [INSTALL] SQL Server 2022 Express...
echo This can take several minutes.
call :install_package "Microsoft.SQLServer.2022.Express" "SQL Server 2022 Express"
if errorlevel 1 (
    echo [ERROR] winget could not install SQL Server Express.
    echo If a SQL Server Installation Center window is open, close it and run this file again.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-Service MSSQLSERVER -ErrorAction SilentlyContinue) { exit 0 } elseif (Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    echo [ERROR] SQL Server service was not found after winget install.
    echo Open Windows Apps list and check if SQL Server Express was installed.
    exit /b 1
)

:configure_direct_sql_server
echo [SETUP] Configuring SQL Server TCP/IP port %DB_PORT%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$service = Get-Service MSSQLSERVER -ErrorAction SilentlyContinue;" ^
  "$instance = 'MSSQLSERVER';" ^
  "if (!$service) { $service = Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue; $instance = 'SQLEXPRESS' };" ^
  "if (!$service) { throw 'SQL Server service was not found after install.' };" ^
  "$instanceNames = Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL' -ErrorAction Stop;" ^
  "$instanceKey = $instanceNames.$instance;" ^
  "if ([string]::IsNullOrWhiteSpace($instanceKey)) { throw ('Could not resolve registry key for SQL instance ' + $instance) };" ^
  "$tcpRoot='HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\' + $instanceKey + '\MSSQLServer\SuperSocketNetLib\Tcp';" ^
  "$ipAll=Join-Path $tcpRoot 'IPAll';" ^
  "if (Test-Path $tcpRoot) { Set-ItemProperty -Path $tcpRoot -Name Enabled -Value 1 };" ^
  "if (Test-Path $ipAll) { Set-ItemProperty -Path $ipAll -Name TcpDynamicPorts -Value ''; Set-ItemProperty -Path $ipAll -Name TcpPort -Value $env:DB_PORT };" ^
  "Set-Service $service.Name -StartupType Automatic;" ^
  "Restart-Service $service.Name -Force;" ^
  "$ready=$false; 1..60 | ForEach-Object { try { $c=New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',[int]$env:DB_PORT); $c.Close(); $ready=$true; break } catch { Start-Sleep -Seconds 2 } }; if (!$ready) { throw ('SQL Server did not listen on port ' + $env:DB_PORT) };" ^
  "$server='127.0.0.1,' + $env:DB_PORT;" ^
  "$windowsBuilder=New-Object System.Data.SqlClient.SqlConnectionStringBuilder; $windowsBuilder['Data Source']=$server; $windowsBuilder['Initial Catalog']='master'; $windowsBuilder['Integrated Security']=$true; $windowsBuilder['Encrypt']=$true; $windowsBuilder['TrustServerCertificate']=$true; $windowsBuilder['Connect Timeout']=30;" ^
  "$conn=[System.Data.SqlClient.SqlConnection]::new($windowsBuilder.ConnectionString);" ^
  "$escapedPassword=$env:DB_PASSWORD.Replace('''','''''');" ^
  "$sql='EXEC xp_instance_regwrite N''HKEY_LOCAL_MACHINE'', N''Software\Microsoft\MSSQLServer\MSSQLServer'', N''LoginMode'', REG_DWORD, 2; ALTER LOGIN [sa] ENABLE; ALTER LOGIN [sa] WITH CHECK_POLICY = OFF; ALTER LOGIN [sa] WITH CHECK_EXPIRATION = OFF; ALTER LOGIN [sa] WITH PASSWORD=N''' + $escapedPassword + ''';';" ^
  "$conn.Open(); $cmd=$conn.CreateCommand(); $cmd.CommandTimeout=120; $cmd.CommandText=$sql; [void]$cmd.ExecuteNonQuery(); $conn.Close();" ^
  "Restart-Service $service.Name -Force;" ^
  "Start-Sleep -Seconds 10;" ^
  "$sqlBuilder=New-Object System.Data.SqlClient.SqlConnectionStringBuilder; $sqlBuilder['Data Source']=$server; $sqlBuilder['Initial Catalog']='master'; $sqlBuilder['User ID']='sa'; $sqlBuilder['Password']=$env:DB_PASSWORD; $sqlBuilder['Encrypt']=$true; $sqlBuilder['TrustServerCertificate']=$true; $sqlBuilder['Connect Timeout']=30;" ^
  "$sqlConn=[System.Data.SqlClient.SqlConnection]::new($sqlBuilder.ConnectionString);" ^
  "$sqlConn.Open(); $sqlConn.Close()"

if errorlevel 1 (
    echo [ERROR] Could not configure/start SQL Server on port %DB_PORT%.
    exit /b 1
)

echo [OK] SQL Server is installed and listening on port %DB_PORT%.
exit /b 0

:failed
echo.
echo Installation did not complete. Fix the error above and run this file again.
pause
exit /b 1
