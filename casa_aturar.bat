@echo off
setlocal enabledelayedexpansion
title JJAUX - Aturar Serveis Casa

echo ==========================================================
echo           ATURANT SERVEIS JJAUX (CASA)
echo ==========================================================

REM Alliberar port 4300 (Frontend)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":4300 "') do (
    if not "%%a"=="0" taskkill /F /PID %%a >nul 2>nul
)
echo   [OK] Frontend aturat (port 4300).

REM Alliberar port 8080 (Backend)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 "') do (
    if not "%%a"=="0" taskkill /F /PID %%a >nul 2>nul
)
echo   [OK] Backend aturat (port 8080).

echo   [OK] PostgreSQL mantingut actiu.
echo ==========================================================
echo Serveis aturats correctament.
echo ==========================================================
