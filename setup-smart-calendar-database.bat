@echo off
setlocal EnableExtensions

set "ROOT_DIR=%~dp0"
set "ENV_FILE=%ROOT_DIR%backend\SmartCalendarAPI\.env"
set "CONTAINER_NAME=TLANDROIDserver"

echo ============================================================
echo   Smart Calendar Database Setup
echo ============================================================
echo.

if not exist "%ENV_FILE%" (
    echo [ERROR] Missing backend env file:
    echo   %ENV_FILE%
    exit /b 1
)

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%A"=="" set "%%A=%%B"
)

if "%HYPERVISOR%"=="" set "HYPERVISOR=docker"
if "%DB_HOST%"=="" set "DB_HOST=localhost"
if "%DB_PORT%"=="" set "DB_PORT=6969"
if "%DB_NAME%"=="" set "DB_NAME=SmartCalendarDB"
if "%DB_USERNAME%"=="" set "DB_USERNAME=sa"

if "%DB_PASSWORD%"=="" (
    echo [ERROR] Missing DB_PASSWORD in:
    echo   %ENV_FILE%
    exit /b 1
)

set "SQL_FILE=%TEMP%\smart_calendar_database_setup_%RANDOM%%RANDOM%.sql"

echo [INFO] HYPERVISOR=%HYPERVISOR%
echo [INFO] Database=%DB_NAME%
echo [INFO] Host=%DB_HOST%
echo [INFO] Port=%DB_PORT%
echo.
echo [INFO] Generating SQL script...

> "%SQL_FILE%" echo SET NOCOUNT ON;
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo EXEC sp_configure 'show advanced options', 1;
>> "%SQL_FILE%" echo RECONFIGURE;
>> "%SQL_FILE%" echo EXEC sp_configure 'max server memory (MB)', 2147483647;
>> "%SQL_FILE%" echo RECONFIGURE;
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF DB_ID(N'%DB_NAME%') IS NULL
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     CREATE DATABASE [%DB_NAME%];
>> "%SQL_FILE%" echo     PRINT 'Database created: %DB_NAME%';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo ELSE
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     PRINT 'Database already exists: %DB_NAME%';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo USE [%DB_NAME%];
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF OBJECT_ID(N'dbo.users', N'U') IS NULL
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     CREATE TABLE dbo.users (
>> "%SQL_FILE%" echo         id INT IDENTITY(1,1) PRIMARY KEY,
>> "%SQL_FILE%" echo         username NVARCHAR(100) NOT NULL UNIQUE,
>> "%SQL_FILE%" echo         email NVARCHAR(255) NOT NULL UNIQUE,
>> "%SQL_FILE%" echo         full_name NVARCHAR(255) NULL
>> "%SQL_FILE%" echo     );
>> "%SQL_FILE%" echo     PRINT 'Table created: users';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF OBJECT_ID(N'dbo.accounts', N'U') IS NULL
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     CREATE TABLE dbo.accounts (
>> "%SQL_FILE%" echo         id INT IDENTITY(1,1) PRIMARY KEY,
>> "%SQL_FILE%" echo         user_id INT NOT NULL UNIQUE,
>> "%SQL_FILE%" echo         username NVARCHAR(100) NOT NULL UNIQUE,
>> "%SQL_FILE%" echo         login_name NVARCHAR(100) NOT NULL UNIQUE,
>> "%SQL_FILE%" echo         password_hash NVARCHAR(255) NOT NULL,
>> "%SQL_FILE%" echo         created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
>> "%SQL_FILE%" echo     );
>> "%SQL_FILE%" echo     PRINT 'Table created: accounts';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF OBJECT_ID(N'dbo.sessions', N'U') IS NULL
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     CREATE TABLE dbo.sessions (
>> "%SQL_FILE%" echo         id INT IDENTITY(1,1) PRIMARY KEY,
>> "%SQL_FILE%" echo         session_token NVARCHAR(255) NOT NULL UNIQUE,
>> "%SQL_FILE%" echo         account_id INT NOT NULL,
>> "%SQL_FILE%" echo         created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
>> "%SQL_FILE%" echo         expires_at DATETIME2 NOT NULL,
>> "%SQL_FILE%" echo         active BIT NOT NULL DEFAULT 1
>> "%SQL_FILE%" echo     );
>> "%SQL_FILE%" echo     PRINT 'Table created: sessions';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF OBJECT_ID(N'dbo.tags', N'U') IS NULL
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     CREATE TABLE dbo.tags (
>> "%SQL_FILE%" echo         id INT IDENTITY(1,1) PRIMARY KEY,
>> "%SQL_FILE%" echo         name NVARCHAR(100) NOT NULL,
>> "%SQL_FILE%" echo         color NVARCHAR(50) NULL,
>> "%SQL_FILE%" echo         user_id INT NULL
>> "%SQL_FILE%" echo     );
>> "%SQL_FILE%" echo     PRINT 'Table created: tags';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF OBJECT_ID(N'dbo.events', N'U') IS NULL
>> "%SQL_FILE%" echo BEGIN
>> "%SQL_FILE%" echo     CREATE TABLE dbo.events (
>> "%SQL_FILE%" echo         id INT IDENTITY(1,1) PRIMARY KEY,
>> "%SQL_FILE%" echo         title NVARCHAR(255) NOT NULL,
>> "%SQL_FILE%" echo         description NVARCHAR(MAX) NULL,
>> "%SQL_FILE%" echo         start_time DATETIME2 NOT NULL,
>> "%SQL_FILE%" echo         end_time DATETIME2 NOT NULL,
>> "%SQL_FILE%" echo         tag_id INT NULL,
>> "%SQL_FILE%" echo         user_id INT NULL
>> "%SQL_FILE%" echo     );
>> "%SQL_FILE%" echo     PRINT 'Table created: events';
>> "%SQL_FILE%" echo END
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.users', N'full_name') IS NULL ALTER TABLE dbo.users ADD full_name NVARCHAR(255) NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.accounts', N'user_id') IS NULL ALTER TABLE dbo.accounts ADD user_id INT NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.accounts', N'username') IS NULL ALTER TABLE dbo.accounts ADD username NVARCHAR(100) NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.accounts', N'login_name') IS NULL ALTER TABLE dbo.accounts ADD login_name NVARCHAR(100) NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.accounts', N'password_hash') IS NULL ALTER TABLE dbo.accounts ADD password_hash NVARCHAR(255) NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.accounts', N'created_at') IS NULL ALTER TABLE dbo.accounts ADD created_at DATETIME2 NOT NULL CONSTRAINT df_accounts_created_at DEFAULT SYSUTCDATETIME();
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.sessions', N'session_token') IS NULL ALTER TABLE dbo.sessions ADD session_token NVARCHAR(255) NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.sessions', N'account_id') IS NULL ALTER TABLE dbo.sessions ADD account_id INT NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.sessions', N'created_at') IS NULL ALTER TABLE dbo.sessions ADD created_at DATETIME2 NOT NULL CONSTRAINT df_sessions_created_at DEFAULT SYSUTCDATETIME();
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.sessions', N'expires_at') IS NULL ALTER TABLE dbo.sessions ADD expires_at DATETIME2 NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.sessions', N'active') IS NULL ALTER TABLE dbo.sessions ADD active BIT NOT NULL CONSTRAINT df_sessions_active DEFAULT 1;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.tags', N'user_id') IS NULL ALTER TABLE dbo.tags ADD user_id INT NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.events', N'user_id') IS NULL ALTER TABLE dbo.events ADD user_id INT NULL;
>> "%SQL_FILE%" echo IF COL_LENGTH(N'dbo.events', N'tag_id') IS NULL ALTER TABLE dbo.events ADD tag_id INT NULL;
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_accounts_users')
>> "%SQL_FILE%" echo     ALTER TABLE dbo.accounts ADD CONSTRAINT fk_accounts_users FOREIGN KEY (user_id) REFERENCES dbo.users(id);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_sessions_accounts')
>> "%SQL_FILE%" echo     ALTER TABLE dbo.sessions ADD CONSTRAINT fk_sessions_accounts FOREIGN KEY (account_id) REFERENCES dbo.accounts(id);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_tags_users')
>> "%SQL_FILE%" echo     ALTER TABLE dbo.tags ADD CONSTRAINT fk_tags_users FOREIGN KEY (user_id) REFERENCES dbo.users(id);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_events_tags')
>> "%SQL_FILE%" echo     ALTER TABLE dbo.events ADD CONSTRAINT fk_events_tags FOREIGN KEY (tag_id) REFERENCES dbo.tags(id);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_events_users')
>> "%SQL_FILE%" echo     ALTER TABLE dbo.events ADD CONSTRAINT fk_events_users FOREIGN KEY (user_id) REFERENCES dbo.users(id);
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'arufa')
>> "%SQL_FILE%" echo     INSERT INTO dbo.users (username, email, full_name) VALUES (N'arufa', N'arufa@example.com', N'Arufa Nguyen');
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'thang')
>> "%SQL_FILE%" echo     INSERT INTO dbo.users (username, email, full_name) VALUES (N'thang', N'thang@example.com', N'Thang Nguyen');
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'hien')
>> "%SQL_FILE%" echo     INSERT INTO dbo.users (username, email, full_name) VALUES (N'hien', N'hien@example.com', N'Hien Nguyen');
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo DECLARE @ArufaId INT = (SELECT id FROM dbo.users WHERE username = N'arufa');
>> "%SQL_FILE%" echo DECLARE @ThangId INT = (SELECT id FROM dbo.users WHERE username = N'thang');
>> "%SQL_FILE%" echo DECLARE @HienId INT = (SELECT id FROM dbo.users WHERE username = N'hien');
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo UPDATE dbo.accounts SET login_name = username WHERE login_name IS NULL AND username IS NOT NULL;
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE login_name = N'arufa' OR username = N'arufa' OR user_id = @ArufaId)
>> "%SQL_FILE%" echo     INSERT INTO dbo.accounts (user_id, username, login_name, password_hash) VALUES (@ArufaId, N'arufa', N'arufa', N'dev-password-hash-arufa');
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE login_name = N'thang' OR username = N'thang' OR user_id = @ThangId)
>> "%SQL_FILE%" echo     INSERT INTO dbo.accounts (user_id, username, login_name, password_hash) VALUES (@ThangId, N'thang', N'thang', N'dev-password-hash-thang');
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE login_name = N'hien' OR username = N'hien' OR user_id = @HienId)
>> "%SQL_FILE%" echo     INSERT INTO dbo.accounts (user_id, username, login_name, password_hash) VALUES (@HienId, N'hien', N'hien', N'dev-password-hash-hien');
>> "%SQL_FILE%" echo UPDATE dbo.accounts SET password_hash = N'8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92' WHERE password_hash LIKE N'dev-password-hash-%%';
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo DECLARE @ArufaId INT = (SELECT id FROM dbo.users WHERE username = N'arufa');
>> "%SQL_FILE%" echo DECLARE @ThangId INT = (SELECT id FROM dbo.users WHERE username = N'thang');
>> "%SQL_FILE%" echo DECLARE @HienId INT = (SELECT id FROM dbo.users WHERE username = N'hien');
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Study' AND user_id = @ArufaId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Study', N'#2196F3', @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Work' AND user_id = @ArufaId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Work', N'#4CAF50', @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Personal' AND user_id = @ArufaId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Personal', N'#FF9800', @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Health' AND user_id = @ArufaId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Health', N'#E91E63', @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Exam' AND user_id = @ThangId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Exam', N'#9C27B0', @ThangId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Meeting' AND user_id = @ThangId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Meeting', N'#00BCD4', @ThangId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Project' AND user_id = @ThangId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Project', N'#3F51B5', @ThangId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Family' AND user_id = @HienId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Family', N'#795548', @HienId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'Travel' AND user_id = @HienId) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'Travel', N'#009688', @HienId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.tags WHERE name = N'System Default' AND user_id IS NULL) INSERT INTO dbo.tags (name, color, user_id) VALUES (N'System Default', N'#607D8B', NULL);
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo DECLARE @ArufaId INT = (SELECT id FROM dbo.users WHERE username = N'arufa');
>> "%SQL_FILE%" echo DECLARE @ThangId INT = (SELECT id FROM dbo.users WHERE username = N'thang');
>> "%SQL_FILE%" echo DECLARE @HienId INT = (SELECT id FROM dbo.users WHERE username = N'hien');
>> "%SQL_FILE%" echo DECLARE @StudyTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Study' AND user_id = @ArufaId);
>> "%SQL_FILE%" echo DECLARE @WorkTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Work' AND user_id = @ArufaId);
>> "%SQL_FILE%" echo DECLARE @PersonalTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Personal' AND user_id = @ArufaId);
>> "%SQL_FILE%" echo DECLARE @HealthTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Health' AND user_id = @ArufaId);
>> "%SQL_FILE%" echo DECLARE @ExamTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Exam' AND user_id = @ThangId);
>> "%SQL_FILE%" echo DECLARE @MeetingTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Meeting' AND user_id = @ThangId);
>> "%SQL_FILE%" echo DECLARE @ProjectTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Project' AND user_id = @ThangId);
>> "%SQL_FILE%" echo DECLARE @FamilyTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Family' AND user_id = @HienId);
>> "%SQL_FILE%" echo DECLARE @TravelTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'Travel' AND user_id = @HienId);
>> "%SQL_FILE%" echo DECLARE @DefaultTag INT = (SELECT TOP 1 id FROM dbo.tags WHERE name = N'System Default' AND user_id IS NULL);
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Hoc Spring Boot' AND user_id = @ArufaId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Hoc Spring Boot', N'Test Smart Calendar API', '2026-06-20T08:00:00', '2026-06-20T10:00:00', @StudyTag, @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'On tap Kotlin' AND user_id = @ArufaId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'On tap Kotlin', N'Lam bai tap MVVM', '2026-06-21T09:00:00', '2026-06-21T11:00:00', @StudyTag, @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Hop nhom backend' AND user_id = @ArufaId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Hop nhom backend', N'Kiem tra API va database', '2026-06-22T14:00:00', '2026-06-22T15:30:00', @WorkTag, @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Di kham suc khoe' AND user_id = @ArufaId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Di kham suc khoe', N'Lich hen phong kham', '2026-06-23T07:30:00', '2026-06-23T09:00:00', @HealthTag, @ArufaId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Nop bao cao chuong 2' AND user_id = @ThangId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Nop bao cao chuong 2', N'Hoan thien WordTL.md', '2026-06-24T10:00:00', '2026-06-24T11:00:00', @ExamTag, @ThangId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Gap giang vien' AND user_id = @ThangId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Gap giang vien', N'Trao doi tien do do an', '2026-06-25T13:00:00', '2026-06-25T14:00:00', @MeetingTag, @ThangId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Demo ung dung' AND user_id = @ThangId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Demo ung dung', N'Chay app Android voi backend', '2026-06-26T15:00:00', '2026-06-26T16:30:00', @ProjectTag, @ThangId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'An toi gia dinh' AND user_id = @HienId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'An toi gia dinh', N'Lich ca nhan', '2026-06-27T18:00:00', '2026-06-27T20:00:00', @FamilyTag, @HienId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Di Da Lat' AND user_id = @HienId) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Di Da Lat', N'Chuan bi hanh ly', '2026-06-28T06:00:00', '2026-06-28T12:00:00', @TravelTag, @HienId);
>> "%SQL_FILE%" echo IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE title = N'Lich mac dinh he thong' AND user_id IS NULL) INSERT INTO dbo.events (title, description, start_time, end_time, tag_id, user_id) VALUES (N'Lich mac dinh he thong', N'Du lieu mau khong gan user', '2026-06-29T08:00:00', '2026-06-29T09:00:00', @DefaultTag, NULL);
>> "%SQL_FILE%" echo GO
>> "%SQL_FILE%" echo.
>> "%SQL_FILE%" echo SELECT 'users' AS table_name, COUNT(*) AS total FROM dbo.users
>> "%SQL_FILE%" echo UNION ALL SELECT 'accounts', COUNT(*) FROM dbo.accounts
>> "%SQL_FILE%" echo UNION ALL SELECT 'sessions', COUNT(*) FROM dbo.sessions
>> "%SQL_FILE%" echo UNION ALL SELECT 'tags', COUNT(*) FROM dbo.tags
>> "%SQL_FILE%" echo UNION ALL SELECT 'events', COUNT(*) FROM dbo.events;
>> "%SQL_FILE%" echo GO

if /I "%HYPERVISOR%"=="direct" goto RUN_DIRECT
goto RUN_DOCKER

:RUN_DOCKER
echo [INFO] Running in Docker mode...
where docker >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker CLI is not installed or not available in PATH.
    del "%SQL_FILE%" >nul 2>nul
    exit /b 1
)

docker info >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker daemon is not running. Open Docker Desktop first.
    del "%SQL_FILE%" >nul 2>nul
    exit /b 1
)

docker inspect "%CONTAINER_NAME%" >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Docker container not found: %CONTAINER_NAME%
    echo Create it first or run install-backend-environment.bat.
    del "%SQL_FILE%" >nul 2>nul
    exit /b 1
)

for /f "tokens=*" %%S in ('docker inspect -f "{{.State.Running}}" "%CONTAINER_NAME%" 2^>nul') do set "CONTAINER_RUNNING=%%S"
if /I not "%CONTAINER_RUNNING%"=="true" (
    echo [INFO] Starting container %CONTAINER_NAME%...
    docker start "%CONTAINER_NAME%"
    if errorlevel 1 (
        echo [ERROR] Cannot start container %CONTAINER_NAME%.
        del "%SQL_FILE%" >nul 2>nul
        exit /b 1
    )
)

echo [INFO] Waiting for SQL Server readiness...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ready = $false; 1..60 | ForEach-Object { docker exec $env:CONTAINER_NAME /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U $env:DB_USERNAME -P $env:DB_PASSWORD -Q 'SELECT 1' *> $null; if ($LASTEXITCODE -eq 0) { $ready = $true; break }; Start-Sleep -Seconds 2 }; if (-not $ready) { exit 1 }"
if errorlevel 1 (
    echo [ERROR] SQL Server did not become ready in container %CONTAINER_NAME%.
    echo Check container logs and verify DB_PASSWORD.
    del "%SQL_FILE%" >nul 2>nul
    exit /b 1
)

echo [INFO] Executing SQL inside Docker container...
cmd /c type "%SQL_FILE%" ^| docker exec -i "%CONTAINER_NAME%" /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U "%DB_USERNAME%" -P "%DB_PASSWORD%" -b
set "EXIT_CODE=%ERRORLEVEL%"
goto FINISH

:RUN_DIRECT
echo [INFO] Running in direct SQL Server mode...
where sqlcmd >nul 2>nul
if errorlevel 1 (
    echo [WARN] sqlcmd is not installed or not available in PATH.
    echo [INFO] Falling back to PowerShell SqlClient...
    goto RUN_DIRECT_POWERSHELL
)

echo [INFO] Executing SQL on %DB_HOST%,%DB_PORT%...
sqlcmd -C -S "%DB_HOST%,%DB_PORT%" -U "%DB_USERNAME%" -P "%DB_PASSWORD%" -i "%SQL_FILE%" -b
set "EXIT_CODE=%ERRORLEVEL%"
if "%EXIT_CODE%"=="0" goto FINISH

echo [WARN] SQL login failed or SQL auth is not ready.
echo [INFO] Trying Windows Authentication to enable sa and continue setup...
set "AUTH_SQL_FILE=%TEMP%\smart_calendar_enable_sa_%RANDOM%%RANDOM%.sql"
> "%AUTH_SQL_FILE%" echo ALTER LOGIN [sa] ENABLE;
>> "%AUTH_SQL_FILE%" echo GO
>> "%AUTH_SQL_FILE%" echo ALTER LOGIN [sa] WITH CHECK_POLICY = OFF;
>> "%AUTH_SQL_FILE%" echo GO
>> "%AUTH_SQL_FILE%" echo ALTER LOGIN [sa] WITH CHECK_EXPIRATION = OFF;
>> "%AUTH_SQL_FILE%" echo GO
>> "%AUTH_SQL_FILE%" echo ALTER LOGIN [sa] WITH PASSWORD = N'%DB_PASSWORD%';
>> "%AUTH_SQL_FILE%" echo GO
>> "%AUTH_SQL_FILE%" echo EXEC xp_instance_regwrite N'HKEY_LOCAL_MACHINE', N'Software\Microsoft\MSSQLServer\MSSQLServer', N'LoginMode', REG_DWORD, 2;
>> "%AUTH_SQL_FILE%" echo GO

sqlcmd -C -S "%DB_HOST%,%DB_PORT%" -E -i "%AUTH_SQL_FILE%" -b
if errorlevel 1 (
    echo [WARN] Could not enable sa with Windows Authentication.
    echo [INFO] Trying database setup with Windows Authentication...
    sqlcmd -C -S "%DB_HOST%,%DB_PORT%" -E -i "%SQL_FILE%" -b
    set "EXIT_CODE=%ERRORLEVEL%"
    del "%AUTH_SQL_FILE%" >nul 2>nul
    goto FINISH
)

del "%AUTH_SQL_FILE%" >nul 2>nul
echo [INFO] Restarting SQL Server service so Mixed Mode takes effect...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$service = Get-Service MSSQLSERVER -ErrorAction SilentlyContinue; if (!$service) { $service = Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue }; if ($service) { Restart-Service $service.Name -Force; Start-Sleep -Seconds 8 }"
echo [INFO] sa login updated. Retrying SQL auth...
sqlcmd -C -S "%DB_HOST%,%DB_PORT%" -U "%DB_USERNAME%" -P "%DB_PASSWORD%" -i "%SQL_FILE%" -b
set "EXIT_CODE=%ERRORLEVEL%"
if "%EXIT_CODE%"=="0" goto FINISH

echo [WARN] SQL auth still failed, using Windows Authentication for setup...
sqlcmd -C -S "%DB_HOST%,%DB_PORT%" -E -i "%SQL_FILE%" -b
set "EXIT_CODE=%ERRORLEVEL%"
goto FINISH

:RUN_DIRECT_POWERSHELL
set "SMART_CALENDAR_SQL_FILE=%SQL_FILE%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$sqlFile = $env:SMART_CALENDAR_SQL_FILE;" ^
  "$server = if ($env:DB_PORT) { $env:DB_HOST + ',' + $env:DB_PORT } else { $env:DB_HOST };" ^
  "Add-Type -AssemblyName System.Data;" ^
  "$sql = Get-Content -LiteralPath $sqlFile -Raw;" ^
  "$batches = [regex]::Split($sql, '(?im)^\s*GO\s*$') | Where-Object { -not [string]::IsNullOrWhiteSpace($_) };" ^
  "function New-SqlConnection([bool]$integrated) { $builder=New-Object System.Data.SqlClient.SqlConnectionStringBuilder; $builder['Data Source']=$server; $builder['Initial Catalog']='master'; $builder['Encrypt']=$true; $builder['TrustServerCertificate']=$true; $builder['Connect Timeout']=30; if($integrated){$builder['Integrated Security']=$true}else{$builder['User ID']=$env:DB_USERNAME;$builder['Password']=$env:DB_PASSWORD}; return [System.Data.SqlClient.SqlConnection]::new($builder.ConnectionString) };" ^
  "function Invoke-Batches($connection,$items) { foreach($batch in $items){ $command=$connection.CreateCommand(); $command.CommandTimeout=120; $command.CommandText=$batch; [void]$command.ExecuteNonQuery() } };" ^
  "$connection = New-SqlConnection $false;" ^
  "try {" ^
  "  try { $connection.Open() } catch {" ^
  "    Write-Host ('[WARN] SQL login failed: ' + $_.Exception.GetBaseException().Message);" ^
  "    Write-Host '[INFO] Trying Windows Authentication to enable Mixed Mode and reset sa...';" ^
  "    $windowsConnection=New-SqlConnection $true;" ^
  "    try { $windowsConnection.Open(); $escapedPassword=$env:DB_PASSWORD.Replace('''',''''''); $repair=$windowsConnection.CreateCommand(); $repair.CommandTimeout=120; $repair.CommandText='EXEC xp_instance_regwrite N''HKEY_LOCAL_MACHINE'', N''Software\Microsoft\MSSQLServer\MSSQLServer'', N''LoginMode'', REG_DWORD, 2; ALTER LOGIN [sa] ENABLE; ALTER LOGIN [sa] WITH CHECK_POLICY = OFF; ALTER LOGIN [sa] WITH CHECK_EXPIRATION = OFF; ALTER LOGIN [sa] WITH PASSWORD=N''' + $escapedPassword + ''';'; [void]$repair.ExecuteNonQuery() } finally { if($windowsConnection.State -ne 'Closed'){$windowsConnection.Close()} };" ^
  "    $service=Get-Service MSSQLSERVER -ErrorAction SilentlyContinue; if(!$service){$service=Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue}; if(!$service){throw 'SQL Server service was not found for restart.'}; Restart-Service $service.Name -Force;" ^
  "    $connected=$false; 1..60 | ForEach-Object { Start-Sleep -Seconds 2; try { $connection.Dispose(); $connection=New-SqlConnection $false; $connection.Open(); $connected=$true; break } catch {} }; if(!$connected){throw 'sa login still failed after enabling Mixed Mode and restarting SQL Server.'}" ^
  "  };" ^
  "  Invoke-Batches $connection $batches;" ^
  "  Write-Host '[INFO] SQL executed successfully via PowerShell SqlClient using SQL authentication.'" ^
  "} finally {" ^
  "  if ($connection.State -ne 'Closed') { $connection.Close() }" ^
  "}"
set "EXIT_CODE=%ERRORLEVEL%"
goto FINISH

:FINISH
if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERROR] Database setup failed. Exit code: %EXIT_CODE%
    echo SQL file kept for debugging:
    echo   %SQL_FILE%
    exit /b %EXIT_CODE%
)

del "%SQL_FILE%" >nul 2>nul
echo.
echo ============================================================
echo   Database setup completed successfully
echo ============================================================
echo.
echo Created/updated:
echo   - Database: %DB_NAME%
echo   - Tables: users, accounts, sessions, tags, events
echo   - Foreign keys: accounts-sessions-users-tags-events
echo   - Seed data: 3 users, 3 accounts, 10 tags, 10 events
echo.
exit /b 0
