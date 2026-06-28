@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "NINEROUTER_PORT=20128"
set "NPM_PREFIX=%~dp0.tools\npm-global"

if not exist "%NPM_PREFIX%\9router.cmd" (
    echo [ERROR] 9router.cmd not found.
    echo Run install-9router.bat first.
    pause
    exit /b 1
)

"%NPM_PREFIX%\9router.cmd" --host 0.0.0.0 --port %NINEROUTER_PORT% --no-browser