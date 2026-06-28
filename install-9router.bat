@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title 9Router Installer

echo ============================================================
echo   9Router Installer
echo ============================================================
echo.

set "NINEROUTER_PORT=20128"
set "NPM_PREFIX=%~dp0.tools\npm-global"
set "NPM_CACHE=%~dp0.tools\npm-cache"

if not exist ".tools" mkdir ".tools"
if not exist "%NPM_PREFIX%" mkdir "%NPM_PREFIX%"
if not exist "%NPM_CACHE%" mkdir "%NPM_CACHE%"

where node >nul 2>&1
if errorlevel 1 (
    echo [INSTALL] Node.js LTS...
    where winget >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Node.js is missing and winget is not available.
        echo Install Node.js LTS manually, then run this file again.
        exit /b 1
    )
    winget install --id OpenJS.NodeJS.LTS --exact --silent --accept-package-agreements --accept-source-agreements --disable-interactivity
    if errorlevel 1 (
        echo [ERROR] Failed to install Node.js LTS.
        exit /b 1
    )
)

for /f "usebackq delims=" %%P in (`powershell -NoProfile -Command "[Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')"`) do set "PATH=%%P"
set "PATH=%NPM_PREFIX%;%PATH%"

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not available in PATH after install.
    exit /b 1
)

where npm.cmd >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm.cmd is not available in PATH.
    exit /b 1
)

echo [INFO] Node:
node --version
echo [INFO] npm:
call npm.cmd --version

echo.
echo [SETUP] Configuring local npm prefix/cache...
call npm.cmd config set prefix "%NPM_PREFIX%"
call npm.cmd config set cache "%NPM_CACHE%"
if errorlevel 1 (
    echo [ERROR] Failed to configure npm prefix/cache.
    exit /b 1
)

echo.
where 9router >nul 2>&1
if errorlevel 1 (
    echo [INSTALL] 9router...
    call npm.cmd install -g 9router
    if errorlevel 1 (
        echo [ERROR] Failed to install 9router.
        exit /b 1
    )
) else (
    echo [OK] 9router is already available.
)

set "PATH=%NPM_PREFIX%;%PATH%"
where 9router >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 9router command is not available after install.
    echo Expected in:
    echo   %NPM_PREFIX%
    exit /b 1
)

echo.
echo [OK] 9router installed.
echo.
echo Run 9router:
echo   9router --host 0.0.0.0 --port %NINEROUTER_PORT% --no-browser
echo.
echo Health check:
echo   powershell -NoProfile -Command "Invoke-RestMethod http://localhost:%NINEROUTER_PORT%/api/health"
echo.
echo Dashboard:
echo   http://localhost:%NINEROUTER_PORT%/dashboard
echo.
exit /b 0
