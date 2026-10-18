@echo off
REM ==============================================================================
REM Script per buidar la base de dades a CASA (Windows Batch)
REM ==============================================================================
echo ==========================================================
echo       BUIDANT BASE DE DADES JJAUX_DB - ENTORNO CASA
echo ==========================================================
set PGPASSWORD=postgres
set SPRING_DATASOURCE_PASSWORD=postgres
where node >nul 2>nul
if %ERRORLEVEL% equ 0 (
    node "%~dp0buidar_db.js" casa
) else (
    echo [INFO] Node.js no detectat al PATH, executant buidat amb psql...
    psql -h localhost -p 5432 -U postgres -d jjaux_db -f "%~dp0buidar_db.sql"
)
pause

