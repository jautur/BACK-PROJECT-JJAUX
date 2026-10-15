@echo off
REM ==============================================================================
REM Script per buidar la base de dades a CASA (Windows Batch)
REM ==============================================================================
echo ==========================================================
echo       BUIDANT BASE DE DADES JJAUX_DB - ENTORNO CASA
echo ==========================================================
set PGPASSWORD=postgres
set SPRING_DATASOURCE_PASSWORD=postgres
node "%~dp0buidar_db.js" casa
pause
