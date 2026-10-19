@echo off
REM ==============================================================================
REM Script de desplegament ràpid en local per a CASA - JJAUX (Windows Batch)
REM Inicia:
REM   1. PostgreSQL (Port 5432 / Base de dades jjaux_db)
REM   2. Backend Spring Boot (Port 8080)
REM   3. Frontend Angular (Port 4300)
REM ==============================================================================

setlocal enabledelayedexpansion
title JJAUX - Desplegament Casa

echo ==========================================================
echo           INICIANT DESPLEGAMENT LOCAL JJAUX (CASA)
echo ==========================================================

REM Detectar directoris
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%..\FRONT-PROJECT-JJAUX" (
    set "BACK_DIR=%SCRIPT_DIR%"
    pushd "%SCRIPT_DIR%..\FRONT-PROJECT-JJAUX"
    set "FRONT_DIR=!CD!"
    popd
) else if exist "%SCRIPT_DIR%BACK-PROJECT-JJAUX" (
    set "FRONT_DIR=%SCRIPT_DIR%FRONT-PROJECT-JJAUX"
    set "BACK_DIR=%SCRIPT_DIR%BACK-PROJECT-JJAUX"
) else (
    set "FRONT_DIR=C:\Users\User\Desktop\FRONT-PROJECT-JJAUX"
    set "BACK_DIR=C:\Users\User\Desktop\BACK-PROJECT-JJAUX"
)

REM 1. Comprovar PostgreSQL
echo.
echo [1/3] Comprovant PostgreSQL al port 5432...
netstat -ano | findstr ":5432 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo   -^> PostgreSQL ja esta en marxa al port 5432.
) else (
    echo   -^> Engegant servei PostgreSQL...
    net start postgresql-x64-18 >nul 2>nul
    ping -n 3 127.0.0.1 >nul
)

REM Verificar administrador amb Node
if exist "%FRONT_DIR%\assegurar_admin.js" (
    node "%FRONT_DIR%\assegurar_admin.js"
) else if exist "%BACK_DIR%\assegurar_admin.js" (
    node "%BACK_DIR%\assegurar_admin.js"
)

REM 2. Backend Spring Boot (Port 8080)
echo.
echo [2/3] Comprovant Backend Spring Boot...
set "JAR_FILE=%BACK_DIR%\target\jjaux-back-0.0.1-SNAPSHOT.jar"

if not exist "%JAR_FILE%" (
    echo   -^> Compilant el backend amb Maven...
    cd /d "%BACK_DIR%"
    call mvnw.cmd package -DskipTests
)

netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo   -^> Backend ja esta responent a http://localhost:8080
    goto :check_frontend
)

echo   -^> Engegant Spring Boot en segon pla...
cd /d "%BACK_DIR%"
set SPRING_DATASOURCE_PASSWORD=postgres
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = Start-Process -FilePath 'java' -ArgumentList '-jar', '%JAR_FILE%' -WorkingDirectory '%BACK_DIR%' -RedirectStandardOutput '%BACK_DIR%\backend.log' -RedirectStandardError '%BACK_DIR%\backend_err.log' -WindowStyle Minimized -PassThru; Set-Content -Path '%BACK_DIR%\backend.pid' -Value $p.Id"

echo   -^> Esperant que el backend estigui llest...
for /L %%i in (1,1,25) do (
    curl -s http://localhost:8080/api/dashboard >nul 2>nul
    if !ERRORLEVEL! equ 0 (
        echo   -^> Backend llest!
        goto :check_frontend
    )
    ping -n 2 127.0.0.1 >nul
)

:check_frontend
REM 3. Frontend Angular (Port 4300)
echo.
echo [3/3] Comprovant Frontend Angular...
if not exist "%FRONT_DIR%\dist\front-project-jjaux-1\browser\index.html" (
    echo   -^> Compilant Frontend Angular...
    cd /d "%FRONT_DIR%"
    call npm run build
)

netstat -ano | findstr ":4300 " | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo   -^> Frontend ja esta servint a http://localhost:4300
    goto :open_browser
)

echo   -^> Engegant servidor web del Frontend (port 4300)...
cd /d "%FRONT_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = Start-Process -FilePath 'node' -ArgumentList 'serve.js' -WorkingDirectory '%FRONT_DIR%' -RedirectStandardOutput '%FRONT_DIR%\frontend.log' -RedirectStandardError '%FRONT_DIR%\frontend_err.log' -WindowStyle Minimized -PassThru; Set-Content -Path '%FRONT_DIR%\frontend.pid' -Value $p.Id"
ping -n 3 127.0.0.1 >nul

:open_browser
REM 4. Obrir al navegador
echo.
echo   -^> Obrint aplicacio al navegador...
start "" "http://localhost:4300"

echo ==========================================================
echo            DESPLEGAMENT COMPLETAT AMB EXIT!
echo ==========================================================
echo  - Base de dades: PostgreSQL a localhost:5432 (BD: jjaux_db)
echo  - API Backend:   http://localhost:8080/api
echo  - Aplicacio Web: http://localhost:4300
echo  - Usuari Admin:  admin@admin.es / adminadmin
echo ==========================================================
echo Per aturar tots els serveis: casa_aturar.bat
echo ==========================================================
