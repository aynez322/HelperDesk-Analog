@echo off
REM =====================================================
REM  helpdesk - Fresh Windows Setup (one-click launcher)
REM  Right-click -> Run as Administrator (for winget/Docker)
REM =====================================================
echo.
echo  helpdesk - Fresh Windows Setup
echo  =============================
echo.

REM Try to elevate if not admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo  Requesting admin privileges (needed for winget and Docker)...
    powershell -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

REM Run the PowerShell script
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1"

echo.
pause